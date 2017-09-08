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
package org.apache.cxf.aegis.inheritance.ws2.common.exception;

public class NotFoundException extends Exception {
    private static final long serialVersionUID = 6832746456656999568L;
    private final String id;

    public NotFoundException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }


    public String toString() {
        return "[" + getClass().getName() + "] id=" + id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NotFoundException that = (NotFoundException)o;

        if (getMessage() != null ? !getMessage().equals(that.getMessage()) : that.getMessage() != null) {
            return false;
        }

        return !(id != null ? !id.equals(that.id) : that.id != null);
    }

    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
