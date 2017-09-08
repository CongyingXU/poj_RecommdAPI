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

package org.apache.axis2.corba.idl.types;

import java.util.ArrayList;
import java.util.List;

public class Operation {
	private String name;
	private List params = new ArrayList();
	private DataType returnType;
	private List raises = new ArrayList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getParams() {
        return params;
    }

    public void setParams(List params) {
        this.params = params;
    }

    public void addParam(Member param) {
        params.add(param);
    }

    public DataType getReturnType() {
        return returnType;
    }

    public void setReturnType(DataType returnType) {
        this.returnType = returnType;
    }

    public List getRaises() {
        return raises;
    }

    public boolean hasRaises() {
        return raises != null && (raises.size() > 0);
    }

    public boolean hasOutParams() {
        for (int i = 0; i < params.size(); i++) {
            Member member = (Member) params.get(0);
            if (Member.MODE_OUT.equals(member.getMode())
                    || Member.MODE_INOUT.equals(member.getMode())) {
                return true;
            }
        }
        return false;
    }

    public void setRaises(List raises) {
        this.raises = raises;
    }

    public void addRaises(ExceptionType exceptionType) {
        raises.add(exceptionType);
    }
}
