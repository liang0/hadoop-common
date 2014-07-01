/**
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.fs;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.io.ByteBufferPool;
import org.apache.hadoop.fs.ByteBufferUtil;
import org.apache.hadoop.util.IdentityHashStore;

/** Utility that wraps a {@link FSInputStream} in a {@link DataInputStream}
 * and buffers input through a {@link BufferedInputStream}. */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class FSDataInputStream extends DataInputStream
    implements Seekable, PositionedReadable, Closeable, 
      ByteBufferReadable, HasFileDescriptor, CanSetDropBehind, CanSetReadahead,
      HasEnhancedByteBufferAccess {
  /**
   * Map ByteBuffers that we have handed out to readers to ByteBufferPool 
   * objects
   */
  private final IdentityHashStore<ByteBuffer, ByteBufferPool>
    extendedReadBuffers
      = new IdentityHashStore<ByteBuffer, ByteBufferPool>(0);

  /**
   * Type of file advise to be passed on to the underlying file system. This
   * information can be used to make optimizations such as reclaiming buffers
   * for files that are no longer needed by the application, etc.
   */
  @MapRModified
  public static enum FadviseType {
    FILE_DONTNEED,
    FILE_RANDOM,
    FILE_SEQUENTIAL;
  }

  public FSDataInputStream(InputStream in)
    throws IOException {
    super(in);
    if( !(in instanceof Seekable) || !(in instanceof PositionedReadable) ) {
      throw new IllegalArgumentException(
          "In is not an instance of Seekable or PositionedReadable");
    }
  }
  
  /**
   * Seek to the given offset.
   *
   * @param desired offset to seek to
   */
  @Override
  public synchronized void seek(long desired) throws IOException {
    ((Seekable)in).seek(desired);
  }

  /**
   * Get the current position in the input stream.
   *
   * @return current position in the input stream
   */
  @Override
  public long getPos() throws IOException {
    return ((Seekable)in).getPos();
  }
  
  /**
   * Read bytes from the given position in the stream to the given buffer.
   *
   * @param position  position in the input stream to seek
   * @param buffer    buffer into which data is read
   * @param offset    offset into the buffer in which data is written
   * @param length    maximum number of bytes to read
   * @return total number of bytes read into the buffer, or <code>-1</code>
   *         if there is no more data because the end of the stream has been
   *         reached
   */
  @Override
  public int read(long position, byte[] buffer, int offset, int length)
    throws IOException {
    return ((PositionedReadable)in).read(position, buffer, offset, length);
  }

  /**
   * Read bytes from the given position in the stream to the given buffer.
   * Continues to read until <code>length</code> bytes have been read.
   *
   * @param position  position in the input stream to seek
   * @param buffer    buffer into which data is read
   * @param offset    offset into the buffer in which data is written
   * @param length    the number of bytes to read
   * @throws EOFException If the end of stream is reached while reading.
   *                      If an exception is thrown an undetermined number
   *                      of bytes in the buffer may have been written. 
   */
  @Override
  public void readFully(long position, byte[] buffer, int offset, int length)
    throws IOException {
    ((PositionedReadable)in).readFully(position, buffer, offset, length);
  }
  
  /**
   * See {@link #readFully(long, byte[], int, int)}.
   */
  @Override
  public void readFully(long position, byte[] buffer)
    throws IOException {
    ((PositionedReadable)in).readFully(position, buffer, 0, buffer.length);
  }
  
  /**
   * Seek to the given position on an alternate copy of the data.
   *
   * @param  targetPos  position to seek to
   * @return true if a new source is found, false otherwise
   */
  @Override
  public boolean seekToNewSource(long targetPos) throws IOException {
    return ((Seekable)in).seekToNewSource(targetPos); 
  }
  
  /**
   * Get a reference to the wrapped input stream. Used by unit tests.
   *
   * @return the underlying input stream
   */
  @InterfaceAudience.LimitedPrivate({"HDFS"})
  public InputStream getWrappedStream() {
    return in;
  }

  @Override
  public int read(ByteBuffer buf) throws IOException {
    if (in instanceof ByteBufferReadable) {
      return ((ByteBufferReadable)in).read(buf);
    }

    throw new UnsupportedOperationException("Byte-buffer read unsupported by input stream");
  }

  @Override
  public FileDescriptor getFileDescriptor() throws IOException {
    if (in instanceof HasFileDescriptor) {
      return ((HasFileDescriptor) in).getFileDescriptor();
    } else if (in instanceof FileInputStream) {
      return ((FileInputStream) in).getFD();
    } else {
      return null;
    }
  }

  @Override
  public void setReadahead(Long readahead)
      throws IOException, UnsupportedOperationException {
    try {
      ((CanSetReadahead)in).setReadahead(readahead);
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException(
          "this stream does not support setting the readahead " +
          "caching strategy.");
    }
  }

  @Override
  public void setDropBehind(Boolean dropBehind)
      throws IOException, UnsupportedOperationException {
    try {
      ((CanSetDropBehind)in).setDropBehind(dropBehind);
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("this stream does not " +
          "support setting the drop-behind caching setting.");
    }
  }

  @Override
  public ByteBuffer read(ByteBufferPool bufferPool, int maxLength,
      EnumSet<ReadOption> opts) 
          throws IOException, UnsupportedOperationException {
    try {
      return ((HasEnhancedByteBufferAccess)in).read(bufferPool,
          maxLength, opts);
    }
    catch (ClassCastException e) {
      ByteBuffer buffer = ByteBufferUtil.
          fallbackRead(this, bufferPool, maxLength);
      if (buffer != null) {
        extendedReadBuffers.put(buffer, bufferPool);
      }
      return buffer;
    }
  }

  private static final EnumSet<ReadOption> EMPTY_READ_OPTIONS_SET =
      EnumSet.noneOf(ReadOption.class);

  final public ByteBuffer read(ByteBufferPool bufferPool, int maxLength)
          throws IOException, UnsupportedOperationException {
    return read(bufferPool, maxLength, EMPTY_READ_OPTIONS_SET);
  }
  
  @Override
  public void releaseBuffer(ByteBuffer buffer) {
    try {
      ((HasEnhancedByteBufferAccess)in).releaseBuffer(buffer);
    }
    catch (ClassCastException e) {
      ByteBufferPool bufferPool = extendedReadBuffers.remove( buffer);
      if (bufferPool == null) {
        throw new IllegalArgumentException("tried to release a buffer " +
            "that was not created by this stream.");
      }
      bufferPool.putBuffer(buffer);
    }
  }

  /**
   * Specifies the kind of advise to provide for this stream and the file
   * offsets to which they apply.
   *
   * The default implementation does nothing. Sub classes can override this
   * behavior.
   *
   * @param type advise type
   * @param offset starting file offset
   * @param count number of bytes starting from the offset
   */
  @MapRModified
  public void adviseFile(FadviseType type, long offset, long count)
    throws IOException {
  }

  /**
   * Returns the file length.
   *
   * @return file length
   */
  @MapRModified
  public long getFileLength() throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the file id as string.
   *
   * @return file id as string
   */
  @MapRModified
  public String getFidStr() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the server IPs in which the file is stored. Each IP is stored in a
   * long. For e.g., the first 4 bytes can be used to store the IP in
   * hexadecimal format and the last 4 bytes to store the port number.
   *
   * @return array of server IPs in which the file is stored
   */
  @MapRModified
  public long[] getFidServers() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the file chunk size.
   *
   * @return file chunk size
   */
  @MapRModified
  public long getChunkSize() {
    throw new UnsupportedOperationException();
  }
}
