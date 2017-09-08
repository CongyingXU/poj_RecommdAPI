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

package org.apache.axis2.jaxws.sample;

import javax.xml.ws.BindingProvider;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.axis2.jaxws.framework.AbstractTestCase;
import org.apache.axis2.jaxws.sample.resourceinjection.sei.ResourceInjectionPortType;
import org.apache.axis2.jaxws.sample.resourceinjection.sei.ResourceInjectionService;

public class ResourceInjectionTests extends AbstractTestCase {
    String axisEndpoint = "http://localhost:6060/axis2/services/ResourceInjectionService.ResourceInjectionPortTypeImplPort";
	
    public static Test suite() {
        return getTestSetup(new TestSuite(ResourceInjectionTests.class));
    }
    
    public ResourceInjectionPortType getProxy() {
        ResourceInjectionService service = new ResourceInjectionService();
        ResourceInjectionPortType proxy = service.getResourceInjectionPort();
        BindingProvider p = (BindingProvider) proxy;
        p.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, axisEndpoint);
        return proxy;
    }
    
    /**
     * This test ensures that an endpoint with an inject WebServiceContext
     * can successfully get and query the web service.
     */
    public void testEchoWithResourceInjectionAndLifecycleMethods() throws Exception {
          
            ResourceInjectionPortType proxy = getProxy();
            String response = proxy.testInjection("sample");
            assertTrue("The response was null", response != null);
            assertTrue("The response was not succesful: " + response, 
                       response.indexOf("SUCCESS") >= 0);
            
            // Repeat to verify behavior
            response = proxy.testInjection("sample");
            assertTrue("The response was null", response != null);
            assertTrue("The response was not succesful: " + response, 
                       response.indexOf("SUCCESS") >= 0);
            char[] chars = new char[] {0x15}; // 0x15 is not a valid xml character..and should be filtered
            String insert = new String(chars);
            assertTrue("Illegal characters were not filtered: " + response,
                    response.indexOf(insert) < 0);
        
    }
   
    /*
     * TODO:  This test is currently disabled.  The path tested assumes the webcontainer will
     * receive the request for <EPR_address>/?wsdl and redirect appropriately to either
     * the on-disk WSDL or, if an on-disk WSDL does not exist, a generated WSDL.  The
     * Axis2 test webserver/webcontainer has no such ability currently.  To support this,
     * the test SimpleServer would have to be coded to detect inbound requests appended with
     * /?wsdl and redirect accordingly.
     *
     * See: jaxws/src/org/apache/axis2/jaxws/context/WebServiceContextImpl.java line 146
     */ 
    public void _testResourceInjectionEndpointReference() {
        ResourceInjectionPortType proxy = getProxy();
        String response = proxy.testInjection("epr");
        assertTrue("The response was null", response != null);
        assertTrue("The response was not succesful: " + response, response
                .indexOf("SUCCESS") >= 0);

        // Make sure the EPR contains the fields we expect
        assertTrue("The EPR did not contain EndpointReference", response
                .indexOf("EndpointReference") >= 0);
        assertTrue("The EPR did not contain ServiceName", response
                .indexOf("ServiceName") >= 0);
        assertTrue("The EPR did not contain wsdlLocation", response
                .indexOf("wsdlLocation") >= 0);
    }
   
}
