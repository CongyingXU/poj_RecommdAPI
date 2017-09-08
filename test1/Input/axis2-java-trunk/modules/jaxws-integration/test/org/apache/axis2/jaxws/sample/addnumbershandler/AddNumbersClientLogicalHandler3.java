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

package org.apache.axis2.jaxws.sample.addnumbershandler;

import org.apache.axis2.jaxws.handler.LogicalMessageContext;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.MessageContext;
import java.io.ByteArrayOutputStream;

/*
 * You can't actually specify whether a handler is for client or server,
 * you just have to check in the handleMessage and/or handleFault to make
 * sure what direction we're going.
 */

public class AddNumbersClientLogicalHandler3  implements javax.xml.ws.handler.LogicalHandler<LogicalMessageContext> {

    HandlerTracker tracker = new HandlerTracker(AddNumbersClientLogicalHandler3.class.getSimpleName());
    
    public void close(MessageContext messagecontext) {
        tracker.close();      
    }

    public boolean handleFault(LogicalMessageContext messagecontext) {
        Boolean outbound = (Boolean) messagecontext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        tracker.handleFault(outbound);
        // let's see if we can do this
        LogicalMessage lm = messagecontext.getMessage();
        String s = getStringFromSourcePayload(lm.getPayload());
        tracker.log("RETURNING FALSE", outbound);
        return false;
    }

    public boolean handleMessage(LogicalMessageContext messagecontext) {
        Boolean outbound = (Boolean) messagecontext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        tracker.handleMessage(outbound);
        return true;
    }
    
    private static String getStringFromSourcePayload(Source payload) {
        try {

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer trans = factory.newTransformer();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);

            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.transform(payload, result);

            return new String(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
