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

package org.apache.axis2.jaxws.description.impl;

import junit.framework.TestCase;
import org.apache.axis2.jaxws.description.ParameterDescription;

import javax.xml.ws.Holder;
import java.lang.reflect.Method;
import java.util.List;

public class ParameterDescriptionImplTests extends TestCase {

    public void test1() throws Exception {
        Method method1 = TestInterface.class.getMethod("method1", Holder.class);
        ParameterDescription pdc1 = new ParameterDescriptionImpl(0, method1.getParameterTypes()[0],
                                                                 method1.getGenericParameterTypes()[0],
                                                                 method1.getAnnotations(), null);
        assertNotNull(pdc1);
        assertEquals(List[].class, pdc1.getParameterActualType());

    }

    public void test2() throws Exception {
        Method method2 = TestInterface.class.getMethod("method2", Holder.class);
        ParameterDescription pdc2 = new ParameterDescriptionImpl(0, method2.getParameterTypes()[0],
                                                                 method2.getGenericParameterTypes()[0],
                                                                 method2.getAnnotations(), null);
        assertNotNull(pdc2);
        // FIXME: Need to chase this down with the Harmony folks.
        if (!"DRLVM".equals(System.getProperty("java.vm.name"))) {
            assertEquals(String[].class, pdc2.getParameterActualType());
        }
    }

    public void test3() throws Exception {
        Method method3 = TestInterface.class.getMethod("method3", Holder.class);
        ParameterDescription pdc3 = new ParameterDescriptionImpl(0, method3.getParameterTypes()[0],
                                                                 method3.getGenericParameterTypes()[0],
                                                                 method3.getAnnotations(), null);
        assertNotNull(pdc3);
        assertEquals(List[].class, pdc3.getParameterActualType());
    }

    public void test4() throws Exception {
        Method method4 = TestInterface.class.getMethod("method4", String[].class);
        ParameterDescription pdc4 = new ParameterDescriptionImpl(0, method4.getParameterTypes()[0],
                                                                 method4.getGenericParameterTypes()[0],
                                                                 method4.getAnnotations(), null);
        assertNotNull(pdc4);
        assertEquals(String[].class, pdc4.getParameterActualType());
    }

    public void test5() throws Exception {
        Method method5 = TestInterface.class.getMethod("method5", List[].class);
        ParameterDescription pdc = new ParameterDescriptionImpl(0, method5.getParameterTypes()[0],
                                                                method5.getGenericParameterTypes()[0],
                                                                method5.getAnnotations(), null);
        assertNotNull(pdc);
        assertEquals(List[].class, pdc.getParameterActualType());
    }
}

interface TestInterface {
    String method1(Holder<List<String>[]> foo);

    String method2(Holder<String[]> foo);

    String method3(Holder<List<?>[]> foo);

    String method4(String[] foo);

    String method5(List<String>[] foo);
}