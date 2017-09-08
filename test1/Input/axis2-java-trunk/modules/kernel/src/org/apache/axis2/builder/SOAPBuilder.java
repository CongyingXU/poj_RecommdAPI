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

package org.apache.axis2.builder;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;

import java.io.IOException;
import java.io.InputStream;

public class SOAPBuilder implements MIMEAwareBuilder {

    public OMElement processDocument(InputStream inputStream, String contentType,
                                     MessageContext messageContext) throws AxisFault {
        try {
            String charSetEncoding = (String) messageContext
                    .getProperty(Constants.Configuration.CHARACTER_SET_ENCODING);
            
            // createSOAPModelBuilder takes care of configuring the underlying parser to
            // avoid the security issue described in CVE-2010-1632
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createSOAPModelBuilder(inputStream,
                    charSetEncoding);
            messageContext.setProperty(Constants.BUILDER, builder);
            SOAPEnvelope envelope = (SOAPEnvelope) builder.getDocumentElement();
            BuilderUtil
                    .validateSOAPVersion(BuilderUtil.getEnvelopeNamespace(contentType), envelope);
            BuilderUtil.validateCharSetEncoding(charSetEncoding, builder.getDocument()
                    .getCharsetEncoding(), envelope.getNamespace().getNamespaceURI());
            return envelope;
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }
    }

    public OMElement processMIMEMessage(Attachments attachments, String contentType,
            MessageContext messageContext) throws AxisFault {
        String charSetEncoding =
                BuilderUtil.getCharSetEncoding(attachments.getRootPartContentType());
        if (charSetEncoding == null) {
            charSetEncoding = MessageContext.UTF_8;
        }
        messageContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                                   charSetEncoding);
        
        messageContext.setDoingSwA(true);

        InputStream in = attachments.getRootPartInputStream(false);
        // Only the attachment parts should be accessible; remove the root part
        attachments.removeDataHandler(attachments.getRootPartContentID());
        messageContext.setAttachmentMap(attachments);
        
        return processDocument(in, contentType, messageContext);
    }
}
