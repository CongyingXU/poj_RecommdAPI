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

package org.apache.axis2.engine;

import junit.framework.TestCase;
import org.apache.axis2.description.WSDL2Constants;

public class DispatchPhaseTest extends TestCase {
    
    /**
     * Verify capability of isOneway method in DispatchPhase.
     */
    @SuppressWarnings("deprecation")
    public void testIsOneway() {
        String mep = WSDL2Constants.MEP_URI_IN_ONLY;
        DispatchPhase phase = new DispatchPhase();
        boolean oneway = phase.isOneway(mep);
        assertTrue(oneway);
        mep = WSDL2Constants.MEP_URI_IN_ONLY;
        oneway = phase.isOneway(mep);
        assertTrue(oneway);
        mep = WSDL2Constants.MEP_URI_IN_ONLY;
        oneway = phase.isOneway(mep);
        assertTrue(oneway);
        mep = WSDL2Constants.MEP_URI_IN_OUT;
        oneway = phase.isOneway(mep);
        assertFalse(oneway);
    }

}
