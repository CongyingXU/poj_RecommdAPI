/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.clustering;

/**
 * A representation of a membership scheme such as "multicast based" or "well-known address (WKA)
 * based" schemes. This is directly related to the membership discovery mechanism.
 */
public interface MembershipScheme {

    /**
     * Initialize this membership scheme
     *
     * @throws ClusteringFault If an error occurs while initializing
     */
    void init() throws ClusteringFault;

    /**
     * JOIN the group
     *
     * @throws ClusteringFault If an error occurs while joining the group
     */
    void joinGroup() throws ClusteringFault;

}
