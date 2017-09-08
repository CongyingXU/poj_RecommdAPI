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

package org.apache.axis2.description.java2wsdl.bytecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class retrieves function parameter names from bytecode built with
 * debugging symbols.  Used as a last resort when creating WSDL.
 */
public class ParamNameExtractor {

	private static final Log log = LogFactory.getLog(ParamNameExtractor.class);

    /**
     * Retrieves a list of function parameter names from a method. 
     * Returns null if unable to read parameter names (i.e. bytecode not
     * built with debug).
     */
    public static String[] getParameterNamesFromDebugInfo(Method method) {
        // Don't worry about it if there are no params.
        int numParams = method.getParameterTypes().length;
        if (numParams == 0)
            return null;

        // get declaring class
        Class c = method.getDeclaringClass();

        // Don't worry about it if the class is a Java dynamic proxy
        if (Proxy.isProxyClass(c)) {
            return null;
        }

        try {
            // get a parameter reader
            ParamReader pr = new ParamReader(c);
            // get the parameter names
            return pr.getParameterNames(method);
        } catch (IOException e) {
            // log it and leave
            return null;
        }
    }
}
