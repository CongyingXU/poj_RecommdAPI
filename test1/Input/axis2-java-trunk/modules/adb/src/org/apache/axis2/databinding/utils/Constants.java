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

package org.apache.axis2.databinding.utils;

import org.apache.axis2.databinding.utils.reader.OMAttribKey;
import org.apache.axis2.databinding.utils.reader.OMElementKey;

public interface Constants {

    static String NIL = "nil";
    static String TRUE = "true";
    static String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    static String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    static String XSI_TYPE_ATTRIBUTE = "type";
    static String DEFAULT_XSI_NAMESPACE_PREFIX = "xsi";
    static String DEFAULT_XSD_NAMESPACE_PREFIX = "xs";
    
    public static String INNER_ARRAY_COMPLEX_TYPE_NAME = "array";
    public static String RETURN_WRAPPER = "return";

    static Object OM_ATTRIBUTE_KEY = new OMAttribKey();
    static Object OM_ELEMENT_KEY = new OMElementKey();
}
