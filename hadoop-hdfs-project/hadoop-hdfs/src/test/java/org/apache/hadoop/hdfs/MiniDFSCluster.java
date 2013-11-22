/**
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
package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.ha.ServiceFailedException;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants.StartupOption;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;

/**
 * This class creates a single-process DFS cluster for junit testing.
 * Sub classes of this class provide the actual implementaion to create, start,
 * stop the cluster. Set the Java system property specified by field
 * <code>CLUSTER_TYPE</code> to determine the sub class to instantiate.
 */
@InterfaceAudience.LimitedPrivate({"HBase", "HDFS", "Hive", "MapReduce", "Pig"})
@InterfaceStability.Unstable
public abstract class MiniDFSCluster {
  protected static final Log LOG = LogFactory.getLog(MiniDFSCluster.class);

  /**
   * System property to be set while running the tests. If it is set to mapr,
   * then MiniMapRDFSCluster will be created. By default, MiniHDFSCluster will
   * be created.
   */
  private static final String CLUSTER_TYPE = "clusterType";

  private static final String MAPR = "mapr";

  static { DefaultMetricsSystem.setMiniClusterMode(true); }

  /**
   * Class to construct instances of MiniDFSClusters with specific options.
   */
  public static class Builder {
    protected int nameNodePort = 0;
    protected int nameNodeHttpPort = 0;
    protected final Configuration conf;
    protected int numDataNodes = 1;
    protected boolean format = true;
    protected boolean manageNameDfsDirs = true;
    protected boolean manageNameDfsSharedDirs = true;
    protected boolean enableManagedDfsDirsRedundancy = true;
    protected boolean manageDataDfsDirs = true;
    protected StartupOption option = null;
    protected String[] racks = null;
    protected String [] hosts = null;
    protected long [] simulatedCapacities = null;
    protected String clusterId = null;
    protected boolean waitSafeMode = true;
    protected boolean setupHostsFile = false;
    protected MiniDFSNNTopology nnTopology = null;
    protected boolean checkExitOnShutdown = true;
    protected boolean checkDataNodeAddrConfig = false;
    protected boolean checkDataNodeHostConfig = false;

    public Builder(Configuration conf) {
      this.conf = conf;
    }

    /**
     * Default: 0
     */
    public Builder nameNodePort(int val) {
      this.nameNodePort = val;
      return this;
    }

    /**
     * Default: 0
     */
    public Builder nameNodeHttpPort(int val) {
      this.nameNodeHttpPort = val;
      return this;
    }

    /**
     * Default: 1
     */
    public Builder numDataNodes(int val) {
      this.numDataNodes = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder format(boolean val) {
      this.format = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder manageNameDfsDirs(boolean val) {
      this.manageNameDfsDirs = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder manageNameDfsSharedDirs(boolean val) {
      this.manageNameDfsSharedDirs = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder enableManagedDfsDirsRedundancy(boolean val) {
      this.enableManagedDfsDirsRedundancy = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder manageDataDfsDirs(boolean val) {
      this.manageDataDfsDirs = val;
      return this;
    }

    /**
     * Default: null
     */
    public Builder startupOption(StartupOption val) {
      this.option = val;
      return this;
    }

    /**
     * Default: null
     */
    public Builder racks(String[] val) {
      this.racks = val;
      return this;
    }

    /**
     * Default: null
     */
    public Builder hosts(String[] val) {
      this.hosts = val;
      return this;
    }

    /**
     * Default: null
     */
    public Builder simulatedCapacities(long[] val) {
      this.simulatedCapacities = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder waitSafeMode(boolean val) {
      this.waitSafeMode = val;
      return this;
    }

    /**
     * Default: true
     */
    public Builder checkExitOnShutdown(boolean val) {
      this.checkExitOnShutdown = val;
      return this;
    }

    /**
     * Default: false
     */
    public Builder checkDataNodeAddrConfig(boolean val) {
      this.checkDataNodeAddrConfig = val;
      return this;
    }

    /**
     * Default: false
     */
    public Builder checkDataNodeHostConfig(boolean val) {
      this.checkDataNodeHostConfig = val;
      return this;
    }

    /**
     * Default: null
     */
    public Builder clusterId(String cid) {
      this.clusterId = cid;
      return this;
    }

    /**
     * Default: false
     * When true the hosts file/include file for the cluster is setup
     */
    public Builder setupHostsFile(boolean val) {
      this.setupHostsFile = val;
      return this;
    }

    /**
     * Default: a single namenode.
     * See {@link MiniDFSNNTopology#simpleFederatedTopology(int)} to set up
     * federated nameservices
     */
    public Builder nnTopology(MiniDFSNNTopology topology) {
      this.nnTopology = topology;
      return this;
    }

    /**
     * Construct the actual MiniDFSCluster instance by looking at the system
     * property <code>CLUSTER_TYPE</code>.
     *
     * @return instance of MiniDFSCluster subclass
     */
    public MiniDFSCluster build() throws IOException {
      String clusterType = System.getProperty(CLUSTER_TYPE);

      if (clusterType != null && clusterType.equalsIgnoreCase(MAPR)) {
        LOG.info("Creating MapR cluster");
        return new MiniMapRDFSCluster(this);
      } else {
        LOG.info("Creating HDFS cluster");
        return new MiniHDFSCluster(this);
      }
    }

    /**
     * Construct MiniHDFSCluster
     */
    public MiniHDFSCluster buildHDFS() throws IOException {
      LOG.info("Creating HDFS cluster");
      return new MiniHDFSCluster(this);
    }
  }

  /**
   * wait for the cluster to get out of safemode.
   */
  public abstract void waitClusterUp() throws IOException;

  /**
   * Modify the config and start up additional DataNodes.  The info port for
   * DataNodes is guaranteed to use a free port.
   *
   *  Data nodes can run with the name node in the mini cluster or
   *  a real name node. For example, running with a real name node is useful
   *  when running simulated data nodes with a real name node.
   *  If minicluster's name node is null assume that the conf has been
   *  set with the right address:port of the name node.
   *
   * @param conf the base configuration to use in starting the DataNodes.  This
   *          will be modified as necessary.
   * @param numDataNodes Number of DataNodes to start; may be zero
   * @param manageDfsDirs if true, the data directories for DataNodes will be
   *          created and {@link #DFS_DATANODE_DATA_DIR_KEY} will be set
   *          in the conf
   * @param operation the operation with which to start the DataNodes.  If null
   *          or StartupOption.FORMAT, then StartupOption.REGULAR will be used.
   * @param racks array of strings indicating the rack that each DataNode is on
   * @param hosts array of strings indicating the hostnames for each DataNode
   * @param simulatedCapacities array of capacities of the simulated data nodes
   *
   * @throws IllegalStateException if NameNode has been shutdown
   */
  public abstract void startDataNodes(Configuration conf, int numDataNodes,
                             boolean manageDfsDirs, StartupOption operation,
                             String[] racks, String[] hosts,
                             long[] simulatedCapacities) throws IOException;

  /**
   * Modify the config and start up additional DataNodes.  The info port for
   * DataNodes is guaranteed to use a free port.
   *
   *  Data nodes can run with the name node in the mini cluster or
   *  a real name node. For example, running with a real name node is useful
   *  when running simulated data nodes with a real name node.
   *  If minicluster's name node is null assume that the conf has been
   *  set with the right address:port of the name node.
   *
   * @param conf the base configuration to use in starting the DataNodes.  This
   *          will be modified as necessary.
   * @param numDataNodes Number of DataNodes to start; may be zero
   * @param manageDfsDirs if true, the data directories for DataNodes will be
   *          created and {@link #DFS_DATANODE_DATA_DIR_KEY} will be
   *          set in the conf
   * @param operation the operation with which to start the DataNodes.  If null
   *          or StartupOption.FORMAT, then StartupOption.REGULAR will be used.
   * @param racks array of strings indicating the rack that each DataNode is on
   * @param hosts array of strings indicating the hostnames for each DataNode
   * @param simulatedCapacities array of capacities of the simulated data nodes
   * @param setupHostsFile add new nodes to dfs hosts files
   *
   * @throws IllegalStateException if NameNode has been shutdown
   */
  public abstract void startDataNodes(Configuration conf, int numDataNodes,
                             boolean manageDfsDirs, StartupOption operation,
                             String[] racks, String[] hosts,
                             long[] simulatedCapacities,
                             boolean setupHostsFile) throws IOException;
  /**
   * @see MiniDFSCluster#startDataNodes(Configuration, int, boolean, StartupOption,
   * String[], String[], long[], boolean, boolean, boolean)
   */
  public abstract void startDataNodes(Configuration conf, int numDataNodes,
      boolean manageDfsDirs, StartupOption operation,
      String[] racks, String[] hosts,
      long[] simulatedCapacities,
      boolean setupHostsFile,
      boolean checkDataNodeAddrConfig) throws IOException;

  /**
   * Modify the config and start up additional DataNodes.  The info port for
   * DataNodes is guaranteed to use a free port.
   *
   *  Data nodes can run with the name node in the mini cluster or
   *  a real name node. For example, running with a real name node is useful
   *  when running simulated data nodes with a real name node.
   *  If minicluster's name node is null assume that the conf has been
   *  set with the right address:port of the name node.
   *
   * @param conf the base configuration to use in starting the DataNodes.  This
   *          will be modified as necessary.
   * @param numDataNodes Number of DataNodes to start; may be zero
   * @param manageDfsDirs if true, the data directories for DataNodes will be
   *          created and {@link #DFS_DATANODE_DATA_DIR_KEY} will be
   *          set in the conf
   * @param operation the operation with which to start the DataNodes.  If null
   *          or StartupOption.FORMAT, then StartupOption.REGULAR will be used.
   * @param racks array of strings indicating the rack that each DataNode is on
   * @param hosts array of strings indicating the hostnames for each DataNode
   * @param simulatedCapacities array of capacities of the simulated data nodes
   * @param setupHostsFile add new nodes to dfs hosts files
   * @param checkDataNodeAddrConfig if true, only set DataNode port addresses if not already set in config
   * @param checkDataNodeHostConfig if true, only set DataNode hostname key if not already set in config
   *
   * @throws IllegalStateException if NameNode has been shutdown
   */
  public abstract void startDataNodes(Configuration conf, int numDataNodes,
      boolean manageDfsDirs, StartupOption operation,
      String[] racks, String[] hosts,
      long[] simulatedCapacities,
      boolean setupHostsFile,
      boolean checkDataNodeAddrConfig,
      boolean checkDataNodeHostConfig) throws IOException;

  /**
   * Modify the config and start up the DataNodes.  The info port for
   * DataNodes is guaranteed to use a free port.
   *
   * @param conf the base configuration to use in starting the DataNodes.  This
   *          will be modified as necessary.
   * @param numDataNodes Number of DataNodes to start; may be zero
   * @param manageDfsDirs if true, the data directories for DataNodes will be
   *          created and {@link DFSConfigKeys#DFS_DATANODE_DATA_DIR_KEY} will be
   *          set in the conf
   * @param operation the operation with which to start the DataNodes.  If null
   *          or StartupOption.FORMAT, then StartupOption.REGULAR will be used.
   * @param racks array of strings indicating the rack that each DataNode is on
   *
   * @throws IllegalStateException if NameNode has been shutdown
   */

  public abstract void startDataNodes(Configuration conf, int numDataNodes,
      boolean manageDfsDirs, StartupOption operation,
      String[] racks
      ) throws IOException;

  /**
   * Modify the config and start up additional DataNodes.  The info port for
   * DataNodes is guaranteed to use a free port.
   *
   *  Data nodes can run with the name node in the mini cluster or
   *  a real name node. For example, running with a real name node is useful
   *  when running simulated data nodes with a real name node.
   *  If minicluster's name node is null assume that the conf has been
   *  set with the right address:port of the name node.
   *
   * @param conf the base configuration to use in starting the DataNodes.  This
   *          will be modified as necessary.
   * @param numDataNodes Number of DataNodes to start; may be zero
   * @param manageDfsDirs if true, the data directories for DataNodes will be
   *          created and {@link DFSConfigKeys#DFS_DATANODE_DATA_DIR_KEY} will
   *          be set in the conf
   * @param operation the operation with which to start the DataNodes.  If null
   *          or StartupOption.FORMAT, then StartupOption.REGULAR will be used.
   * @param racks array of strings indicating the rack that each DataNode is on
   * @param simulatedCapacities array of capacities of the simulated data nodes
   *
   * @throws IllegalStateException if NameNode has been shutdown
   */
  public abstract void startDataNodes(Configuration conf, int numDataNodes,
                             boolean manageDfsDirs, StartupOption operation,
                             String[] racks,
                             long[] simulatedCapacities) throws IOException;

  /**
   * Shutdown all the nodes in the cluster.
   */
  public abstract void shutdown();

  /**
   * Shutdown all DataNodes started by this class.  The NameNode
   * is left running so that new DataNodes may be started.
   */
  public abstract void shutdownDataNodes();

  /*
   * Shutdown a particular datanode
   */
  public abstract Object stopDataNode(int i);

  /*
   * Shutdown a datanode by name.
   */
  public abstract Object stopDataNode(String dnName);

  /*
   * Restart a particular datanode, use newly assigned port
   */
  public final boolean restartDataNode(int i) throws IOException {
    return restartDataNode(i, false);
  }

  /*
   * Restart a particular datanode, on the same port if keepPort is true
   */
  public abstract boolean restartDataNode(int i, boolean keepPort)
      throws IOException;

  /*
   * Restart all datanodes, on the same ports if keepPort is true
   */
  public abstract boolean restartDataNodes(boolean keepPort)
      throws IOException;

  /*
   * Restart all datanodes, use newly assigned ports
   */
  public final boolean restartDataNodes() throws IOException {
    return restartDataNodes(false);
  }

  /**
   * Returns true if all the NameNodes are running and is out of Safe Mode.
   */
  public abstract boolean isClusterUp();

  /**
   * Returns true if there is at least one DataNode running.
   */
  public abstract boolean isDataNodeUp();

  /**
   * Get a client handle to the DFS cluster with a single namenode.
   */
  public abstract FileSystem getFileSystem() throws IOException;

  /**
   * Wait until the cluster is active and running.
   */
  public abstract void waitActive() throws IOException;

  public abstract void formatDataNodeDirs() throws IOException;

  /**
   * Shut down a cluster if it is not null
   * @param cluster cluster reference or null
   */
  public final static void shutdownCluster(MiniDFSCluster cluster) {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  /**
   * Returns the URI of the cluster.
   */
  public abstract URI getURI();

  public ArrayList<DataNode> getDataNodes() {
    throw new UnsupportedOperationException();
  }

  /** @return the datanode having the ipc server listen port */
  public DataNode getDataNode(int ipcPort) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the rpc port used by the NameNode, because the caller
   * supplied port is not necessarily the actual port used.
   * Assumption: cluster has a single namenode
   */
  public int getNameNodePort() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the rpc port used by the NameNode at the given index, because the
   * caller supplied port is not necessarily the actual port used.
   */
  public int getNameNodePort(int nnIndex) {
    throw new UnsupportedOperationException();
  }

  public void transitionToActive(int nnIndex) throws IOException,
      ServiceFailedException {
    throw new UnsupportedOperationException();
  }

  public void transitionToStandby(int nnIndex) throws IOException,
      ServiceFailedException {
    throw new UnsupportedOperationException();
  }

  public static String getBaseDirectory() {
    return "";
  }

  /**
   * Return the {@link FSNamesystem} object.
   * @return {@link FSNamesystem} object.
   */
  public FSNamesystem getNamesystem() {
    throw new UnsupportedOperationException();
  }

  public FSNamesystem getNamesystem(int nnIndex) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get an instance of the NameNode's RPC handler.
   */
  public NamenodeProtocols getNameNodeRpc() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get an instance of the NameNode's RPC handler.
   */
  public NamenodeProtocols getNameNodeRpc(int nnIndex) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the started NameNode.  May be null.
   */
  public NameNode getNameNode() {
    throw new UnsupportedOperationException();
  }

  /**
   * Restart all namenodes.
   */
  public synchronized void restartNameNodes() throws IOException {
  }

  /**
   * Restart the namenode.
   */
  public synchronized void restartNameNode() throws IOException {
  }

  /**
   * Restart the namenode. Optionally wait for the cluster to become active.
   */
  public synchronized void restartNameNode(boolean waitActive)
      throws IOException {
  }

  /**
   * Restart the namenode at a given index.
   */
  public synchronized void restartNameNode(int nnIndex) throws IOException {
  }

  /**
   * Restart the namenode at a given index. Optionally wait for the cluster
   * to become active.
   */
  public synchronized void restartNameNode(int nnIndex, boolean waitActive)
      throws IOException {
  }

  /**
   * Set the softLimit and hardLimit of client lease periods
   */
  public void setLeasePeriod(long soft, long hard) {
  }

  public void setLeasePeriod(long soft, long hard, int nnIndex) {
  }
}
