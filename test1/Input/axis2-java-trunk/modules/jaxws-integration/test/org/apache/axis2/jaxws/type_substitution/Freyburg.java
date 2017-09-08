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

package org.apache.axis2.jaxws.type_substitution;

public class Freyburg extends Apple {

    private String season;

    public Freyburg() {
        setType("Freyburg");
    }

    public Freyburg(String color, String flavor, String season) {
        setType("Freyburg");
        setColor(color);
        setFlavor(flavor);
        
        setSeason(season);
    }

    public void setSeason(String arg0) {
        this.season = arg0;
    }

    public String getSeason() {
        return this.season;
    }

}
