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
package org.apache.axis2.databinding.utils.reader;

import java.util.ArrayList;
import java.util.List;

public class DummyBean {
    private List<DummyBean> dependent = new ArrayList<DummyBean>();
    
    public String getName() {
        return "FooTwo";
    }
    
    public String getAge() {
        return "25";
    }
    
    public String getSex() {
        return "Male";
    }
    
    public DummyBean[] getDependent() { return dependent.toArray(new DummyBean[dependent.size()]); }

    public DummyBean addAnotherBean() {
        DummyBean dummyBean = new DummyBean();
        dependent.add(dummyBean);
        return dummyBean;
    }
}
