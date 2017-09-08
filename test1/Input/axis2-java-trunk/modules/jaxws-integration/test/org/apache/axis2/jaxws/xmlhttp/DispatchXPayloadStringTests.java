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

package org.apache.axis2.jaxws.xmlhttp;

import org.apache.axis2.jaxws.TestLogger;
import org.apache.axis2.testutils.Axis2Server;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.http.HTTPBinding;

public class DispatchXPayloadStringTests {
    @ClassRule
    public static Axis2Server server = new Axis2Server("target/repo");
    
    private QName SERVICE_NAME  = new QName("http://ws.apache.org/axis2", "XPayloadStringProvider");
    private QName PORT_NAME  = new QName("http://ws.apache.org/axis2", "XPayloadStringProviderPort");
 
    private static String XML_TEXT = "<p:echo xmlns:p=\"http://sample\">hello world</p:echo>";
    private static String XML_TEXT_NPE = "<p:echo xmlns:p=\"http://sample\">NPE</p:echo>";
    
    public Dispatch<String> getDispatch() {
       Service service = Service.create(SERVICE_NAME);
       service.addPort(PORT_NAME, HTTPBinding.HTTP_BINDING, "http://localhost:" + server.getPort() + "/axis2/services/XPayloadStringProvider.XPayloadStringProviderPort");
       Dispatch<String> dispatch = service.createDispatch(PORT_NAME, String.class, Service.Mode.PAYLOAD);
       return dispatch;
    }
    
    /**
     * Simple XML/HTTP Payload Test
     * @throws Exception
     */
    @Test
    public void testSimple() throws Exception {
        Dispatch<String> dispatch = getDispatch();
        String request = XML_TEXT;
        TestLogger.logger.debug("Request  = " + request);
        String response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
        
        // Try again to verify
        TestLogger.logger.debug("Request  = " + request);
        response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
    }
    
    /**
     * TODO Need to fix the implementation and test
     * @throws Exception
     */
    public void _testEmpty() throws Exception {
        Dispatch<String> dispatch = getDispatch();
        String request = "";
        TestLogger.logger.debug("Request  = " + request);
        String response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
        
        // Try again to verify
        TestLogger.logger.debug("Request  = " + request);
        response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
    }
    
    /**
     * TODO Need to fix the implementation and test
     * @throws Exception
     */
    public void _testNull() throws Exception {
        Dispatch<String> dispatch = getDispatch();
        String request = null;
        TestLogger.logger.debug("Request  = " + request);
        String response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
        
        // Try again to verify
        TestLogger.logger.debug("Request  = " + request);
        response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
    }
    
    /**
     * TODO Need to fix the implementation and test
     * @throws Exception
     */
    public void _testException() throws Exception {
        Dispatch<String> dispatch = getDispatch();
        String request = XML_TEXT_NPE;
        TestLogger.logger.debug("Request  = " + request);
        String response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
        
        // Try again to verify
        TestLogger.logger.debug("Request  = " + request);
        response = dispatch.invoke(request);
        TestLogger.logger.debug("Response = " + response);
        assertTrue(response != null);
        assertTrue(request.equals(response));
    }
}
