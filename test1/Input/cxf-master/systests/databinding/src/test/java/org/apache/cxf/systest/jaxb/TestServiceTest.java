/**
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
package org.apache.cxf.systest.jaxb;


import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.systest.jaxb.model.ExtendedWidget;
import org.apache.cxf.systest.jaxb.model.Widget;
import org.apache.cxf.systest.jaxb.service.TestService;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "classpath:extrajaxbclass.xml" })
public class TestServiceTest extends AbstractJUnit4SpringContextTests {
    static final String PORT = TestUtil.getPortNumber(TestServiceTest.class);

    private TestUtilities testUtilities;

    public TestServiceTest() {
        testUtilities = new TestUtilities(getClass());
    }

    @Test
    public void testExtraSubClassWithJaxb() throws Throwable {
        Widget expected = new ExtendedWidget(42, "blah", "blah", true, true);
        TestService testClient = getTestClient();
        Widget widgetFromService = testClient.getWidgetById(42);

        Assert.assertEquals(expected, widgetFromService);
    }

    @Test
    public void testExtraSubClassWithJaxbFromEndpoint() throws Throwable {
        Widget expected = new ExtendedWidget(42, "blah", "blah", true, true);

        TestService testClient = getTestClient();
        ((BindingProvider)testClient).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 "http://localhost:" + PORT + "/service/TestEndpoint");
        Widget widgetFromService = testClient.getWidgetById(42);

        Assert.assertEquals(expected, widgetFromService);
    }


    @Test
    public void testSchema() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/service/TestService?wsdl");
        String s = IOUtils.toString(url.openStream());
        Assert.assertTrue(s, s.contains("application/octet-stream"));
    }

    @Test
    public void testAutoFaultBeanProperties() throws Exception {
        testUtilities.setBus((Bus)applicationContext.getBean("cxf"));
        testUtilities.addDefaultNamespaces();
        testUtilities.addNamespace("ts", "http://cxf.org.apache/service");
        Server s = testUtilities.getServerForService(new QName("http://cxf.org.apache/service",
                                                               "TestServiceService"));
        Document wsdl = testUtilities.getWSDLDocument(s);
        testUtilities.assertInvalid("//xsd:complexType[@name='TestServiceException']"
                                    + "/xsd:sequence/xsd:element[@name='serialVersionUID']", wsdl);
        testUtilities.assertInvalid("//xsd:complexType[@name='TestServiceException']"
                                    + "/xsd:sequence/xsd:element[@name='privateInt']", wsdl);
        testUtilities.assertValid("//xsd:complexType[@name='TestServiceException']"
                                    + "/xsd:sequence/xsd:element[@name='publicString']", wsdl);
    }


    /**
     * @return the testClient
     */
    public TestService getTestClient() {
        return applicationContext.getBean("testClient", TestService.class);
    }
}
