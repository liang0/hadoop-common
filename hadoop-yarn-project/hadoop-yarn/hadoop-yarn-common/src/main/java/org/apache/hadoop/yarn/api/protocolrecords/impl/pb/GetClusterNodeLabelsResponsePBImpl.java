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

package org.apache.hadoop.yarn.api.protocolrecords.impl.pb;

<<<<<<< HEAD
import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeLabelsResponse;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.GetClusterNodeLabelsResponseProto;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.GetClusterNodeLabelsResponseProtoOrBuilder;

public class GetClusterNodeLabelsResponsePBImpl extends
    GetClusterNodeLabelsResponse {
  Set<String> labels;
  GetClusterNodeLabelsResponseProto proto = GetClusterNodeLabelsResponseProto
      .getDefaultInstance();
  GetClusterNodeLabelsResponseProto.Builder builder = null;
  boolean viaProto = false;

  public GetClusterNodeLabelsResponsePBImpl() {
    this.builder = GetClusterNodeLabelsResponseProto.newBuilder();
=======
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodeLabelsResponse;
import org.apache.hadoop.yarn.api.records.NodeToLabelsList;
import org.apache.hadoop.yarn.api.records.impl.pb.NodeToLabelsListPBImpl;
import org.apache.hadoop.yarn.proto.YarnProtos.NodeToLabelsListProto;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.GetClusterNodeLabelsResponseProto;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.GetClusterNodeLabelsResponseProtoOrBuilder;
import com.google.protobuf.TextFormat;

@Private
@Unstable
public class GetClusterNodeLabelsResponsePBImpl extends
    GetClusterNodeLabelsResponse {

  GetClusterNodeLabelsResponseProto proto =
      GetClusterNodeLabelsResponseProto.getDefaultInstance();
  GetClusterNodeLabelsResponseProto.Builder builder = null;
  boolean viaProto = false;

  List<NodeToLabelsList> clusterNodeLabels;

  public GetClusterNodeLabelsResponsePBImpl() {
    builder = GetClusterNodeLabelsResponseProto.newBuilder();
>>>>>>> MAPR-14424: Implementation for 'mapred job -showlabels' and 'mapred job -refreshlabaels' cli command
  }

  public GetClusterNodeLabelsResponsePBImpl(
      GetClusterNodeLabelsResponseProto proto) {
    this.proto = proto;
    viaProto = true;
  }

<<<<<<< HEAD
  private void maybeInitBuilder() {
    if (viaProto || builder == null) {
      builder = GetClusterNodeLabelsResponseProto.newBuilder(proto);
    }
    viaProto = false;
  }

  private void mergeLocalToBuilder() {
    if (this.labels != null && !this.labels.isEmpty()) {
      builder.clearNodeLabels();
      builder.addAllNodeLabels(this.labels);
=======
  @Override
  public List<NodeToLabelsList> getClusterNodeLabels() {
    initClusterNodeLabels();
    return this.clusterNodeLabels;
  }

  @Override
  public void setClusterNodeLabels(List<NodeToLabelsList> clusterNodeLabels) {
    maybeInitBuilder();
    if (clusterNodeLabels == null) {
      builder.clearNodeToLabels();
    }
    this.clusterNodeLabels = clusterNodeLabels;
  }

  public GetClusterNodeLabelsResponseProto getProto() {
    mergeLocalToProto();
    proto = viaProto ? proto : builder.build();
    viaProto = true;
    return proto;
  }

  @Override
  public int hashCode() {
    return getProto().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other.getClass().isAssignableFrom(this.getClass())) {
      return this.getProto().equals(this.getClass().cast(other).getProto());
    }
    return false;
  }

  @Override
  public String toString() {
    return TextFormat.shortDebugString(getProto());
  }

  private void mergeLocalToBuilder() {
    if (this.clusterNodeLabels != null) {
      addClusterNodeLabelsToProto();
>>>>>>> MAPR-14424: Implementation for 'mapred job -showlabels' and 'mapred job -refreshlabaels' cli command
    }
  }

  private void mergeLocalToProto() {
<<<<<<< HEAD
    if (viaProto)
      maybeInitBuilder();
=======
    if (viaProto) {
      maybeInitBuilder();
    }
>>>>>>> MAPR-14424: Implementation for 'mapred job -showlabels' and 'mapred job -refreshlabaels' cli command
    mergeLocalToBuilder();
    proto = builder.build();
    viaProto = true;
  }

<<<<<<< HEAD
  public GetClusterNodeLabelsResponseProto getProto() {
    mergeLocalToProto();
    proto = viaProto ? proto : builder.build();
    viaProto = true;
    return proto;
  }

  private void initNodeLabels() {
    if (this.labels != null) {
      return;
    }
    GetClusterNodeLabelsResponseProtoOrBuilder p = viaProto ? proto : builder;
    this.labels = new HashSet<String>();
    this.labels.addAll(p.getNodeLabelsList());
  }

  @Override
  public void setNodeLabels(Set<String> labels) {
    maybeInitBuilder();
    if (labels == null || labels.isEmpty()) {
      builder.clearNodeLabels();
    }
    this.labels = labels;
  }

  @Override
  public Set<String> getNodeLabels() {
    initNodeLabels();
    return this.labels;
  }

  @Override
  public int hashCode() {
    assert false : "hashCode not designed";
    return 0;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null)
      return false;
    if (other.getClass().isAssignableFrom(this.getClass())) {
      return this.getProto().equals(this.getClass().cast(other).getProto());
    }
    return false;
  }
}
=======
  private void maybeInitBuilder() {
    if (viaProto || builder == null) {
      builder = GetClusterNodeLabelsResponseProto.newBuilder(proto);
    }
    viaProto = false;
  }

  private void initClusterNodeLabels() {
    if(this.clusterNodeLabels != null) {
      return;
    }

    GetClusterNodeLabelsResponseProtoOrBuilder p = viaProto ? proto : builder;
    List<NodeToLabelsListProto> list = p.getNodeToLabelsList();
    clusterNodeLabels = new ArrayList<NodeToLabelsList>();

    for (NodeToLabelsListProto c : list) {
      clusterNodeLabels.add(convertFromProtoFormat(c));
    }
  }

  private void addClusterNodeLabelsToProto() {
    maybeInitBuilder();
    builder.clearNodeToLabels();
    if (this.clusterNodeLabels == null) {
      return;
    }

    Iterable<NodeToLabelsListProto> iterable =
      new Iterable<NodeToLabelsListProto>() {
		@Override
        public Iterator<NodeToLabelsListProto> iterator() {
          return new Iterator<NodeToLabelsListProto>() {
            Iterator<NodeToLabelsList> iter = clusterNodeLabels.iterator();

            @Override
            public boolean hasNext() {
              return iter.hasNext();
            }

            @Override
            public NodeToLabelsListProto next() {
              return convertToProtoFormat(iter.next());
            }

            @Override
            public void remove() {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
    builder.addAllNodeToLabels(iterable);
  }

  private NodeToLabelsListPBImpl convertFromProtoFormat(NodeToLabelsListProto p) {
    return new NodeToLabelsListPBImpl(p);
  }

  private NodeToLabelsListProto convertToProtoFormat(NodeToLabelsList t) {
    return ((NodeToLabelsListPBImpl) t).getProto();
  }
}

>>>>>>> MAPR-14424: Implementation for 'mapred job -showlabels' and 'mapred job -refreshlabaels' cli command
