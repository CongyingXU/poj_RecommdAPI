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

package org.apache.cxf.service.factory;

import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;

public class ServiceConstructionException extends UncheckedException {

    private static final long serialVersionUID = -4242545895784708313L;
    public ServiceConstructionException(Message msg, Throwable t) {
        super(msg, t);
    }

    public ServiceConstructionException(Message msg) {
        super(msg);
    }

    public ServiceConstructionException(Throwable cause) {
        super(cause);
    }
    public ServiceConstructionException(String msg, Logger log) {
        super(new Message(msg, log));
    }
    public ServiceConstructionException(String msg, Logger log, Throwable cause) {
        super(new Message(msg, log), cause);
    }

}
