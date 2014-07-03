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

package org.apache.hadoop.yarn.server.resourcemanager.scheduler;

import java.util.List;

import net.java.dev.eval.Expression;

import org.apache.hadoop.classification.InterfaceAudience.LimitedPrivate;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.QueueACL;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.QueueUserACLInfo;

@Evolving
@LimitedPrivate("yarn")
public interface Queue {
  /**
   * Get the queue name
   * @return queue name
   */
  String getQueueName();

  /**
   * Get the queue metrics
   * @return the queue metrics
   */
  QueueMetrics getMetrics();

  /**
   * Get queue information
   * @param includeChildQueues include child queues?
   * @param recursive recursively get child queue information?
   * @return queue information
   */
  QueueInfo getQueueInfo(boolean includeChildQueues, boolean recursive);
  
  /**
   * Get queue ACLs for given <code>user</code>.
   * @param user username
   * @return queue ACLs for user
   */
  List<QueueUserACLInfo> getQueueUserAclInfo(UserGroupInformation user);

  boolean hasAccess(QueueACL acl, UserGroupInformation user);
  
  public ActiveUsersManager getActiveUsersManager();
  
  static enum QueueLabelPolicy {
    PREFER_QUEUE("PREFER_QUEUE"), // Queue label always wins
    PREFER_APP("PREFER_APP"), // App label always wins
    AND("AND"),  // Use && on Queue and App labels
    OR("OR");  // Use || on Queue and App labels

    private final String policyName;

    QueueLabelPolicy(String policyName) {
      this.policyName = policyName;
    }

    public String getLabelPolicyName() {
      return policyName;
    }
  }

  public QueueLabelPolicy getLabelPolicy();

  public Expression getLabel();

}
