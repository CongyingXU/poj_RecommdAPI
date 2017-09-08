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

package org.apache.axis2.generics;

import java.util.List;
import java.util.ArrayList;

public class GenericService {

    public String processStringList(List<String> values) {
        return values.get(0);
    }

    public List<String> getStringList() {
        List<String> values = new ArrayList<String>();
        values.add("test1");
        values.add("test2");
        values.add("test3");
        return values;
    }

    public Person processPersonList(List<Person> persons) {
        return persons.get(0);
    }

// See generics.wsdl
/*
    public void processStringArray(List<String[]> values) {

    }
*/
}
