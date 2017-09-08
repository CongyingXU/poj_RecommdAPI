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
package org.apache.cxf.aegis.inheritance.ws1;

public class BeanB extends BeanA {
    private static final long serialVersionUID = 8140920723297559985L;
    private String propB;

    public String getPropB() {
        return propB;
    }

    public void setPropB(String propB) {
        this.propB = propB;
    }

    public String toString() {
        return super.toString() + " ; propB=" + propB;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final BeanB beanB = (BeanB)o;

        return !((propB != null) ? (!propB.equals(beanB.propB)) : (beanB.propB != null));
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (propB != null ? propB.hashCode() : 0);
        return result;
    }
}
