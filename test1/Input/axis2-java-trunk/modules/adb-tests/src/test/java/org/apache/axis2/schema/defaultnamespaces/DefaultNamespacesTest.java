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

package org.apache.axis2.schema.defaultnamespaces;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;

public class DefaultNamespacesTest extends TestCase {
    private static final String NS_URI = TestElement1.MY_QNAME.getNamespaceURI();

    public void testTestElement1() throws Exception {

        TestElement1 testElement1 = new TestElement1();

        TestChildType testChildType = new TestChildType();
        testChildType.setParam1(new QName(NS_URI, "param1"));
        testChildType.setParam2("Param2");
        testChildType.setParam3(new QName(NS_URI, "param3"));
        testChildType.setParam4("Param4");

        TestSimpleUnion testSimpleUnion1 = new TestSimpleUnion();
        testSimpleUnion1.setObject(new QName(NS_URI, "param5"));

        testChildType.setParam5(testSimpleUnion1);

        testChildType.setAttribute1("attribute1");
        testChildType.setAttribute2(new QName(NS_URI, "attribute2"));

        TestSimpleUnion testSimpleUnion2 = new TestSimpleUnion();
        testSimpleUnion2.setObject(new QName(NS_URI, "attribute3"));


        testElement1.setTestElement1(testChildType);

        OMElement omElement = testElement1.getTestElement1().getOMElement(new QName(NS_URI, "TestElement1", "ns1"),
                OMAbstractFactory.getOMFactory());
        TestElement1 result = TestElement1.Factory.parse(omElement.getXMLStreamReader());
        assertTrue(result.getTestElement1() instanceof TestChildType);
        TestChildType resultType = (TestChildType) result.getTestElement1();
        assertEquals(resultType.getParam1(), new QName(NS_URI, "param1"));
        assertEquals(resultType.getParam2(), "Param2");
        assertEquals(resultType.getParam3(), new QName(NS_URI, "param3"));
        assertEquals(resultType.getParam4(), "Param4");
        assertEquals(resultType.getAttribute1(), "attribute1");
        assertEquals(resultType.getAttribute2(), new QName(NS_URI, "attribute2"));
    }
}
