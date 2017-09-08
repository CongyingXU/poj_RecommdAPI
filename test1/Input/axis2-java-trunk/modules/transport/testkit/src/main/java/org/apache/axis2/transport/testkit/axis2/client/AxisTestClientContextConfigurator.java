/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.axis2.transport.testkit.axis2.client;

import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;

/**
 * Resource used to determine whether a transport listener is required and to configure the
 * transport.
 */
public interface AxisTestClientContextConfigurator {
    /**
     * Determine whether a transport listener is required on client side.
     * 
     * @return true if a transport listener instance is required
     */
    boolean isTransportListenerRequired();
    
    /**
     * Setup the transport on client side.
     * 
     * @param trpInDesc
     * @param trpOutDesc
     */
    void setupTransport(TransportInDescription trpInDesc, TransportOutDescription trpOutDesc) throws Exception;
}
