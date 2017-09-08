/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2.tiles.annotation;

/**
 * Represents a <code>&lt;put-list-attribute&gt;</code> element in <code>tiles.xml</code>.
 *
 */
public @interface TilesPutListAttribute {
    boolean cascade() default false;
    boolean inherit() default false;
    String name() default "";
    String role() default "";
    TilesAddAttribute[] addAttributes() default {};
    TilesAddListAttribute[] addListAttributes() default {};
}
