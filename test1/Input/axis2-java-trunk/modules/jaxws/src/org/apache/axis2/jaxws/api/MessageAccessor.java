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
package org.apache.axis2.jaxws.api;

import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.message.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Value of the {@link org.apache.axis2.jaxws.Constants#JAXWS_MESSAGE_ACCESSOR} property.
 * Allows a user to gain access to certain Message information
 * that are not exposed by the Message on the
 * javax.xml.ws.handler.MessageContext
 * 
 * The MessageAccessor is created with MessageAccessorFactory.
 * This allows embedding software to extend the MessageAccessor
 */
public class MessageAccessor {
    private static final Log log = LogFactory.getLog(MessageAccessor.class);
    private MessageContext mc;
    
    MessageAccessor(MessageContext mc) {
        super();
        this.mc = mc;
    }

    /**
     * @return message as String
     */
    public String getMessageAsString() {
        if (log.isDebugEnabled()) {
            log.debug("Enter MessageAccessor");
        }
        Message msg = mc.getMessage();
        String text = null;
        
        if (msg != null) {
            try {
                text = msg.getAsOMElement().toString();
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot access message as string", t);
                }
                text = null;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Exit MessageAccessor");
        }
        return text;
    }
}
