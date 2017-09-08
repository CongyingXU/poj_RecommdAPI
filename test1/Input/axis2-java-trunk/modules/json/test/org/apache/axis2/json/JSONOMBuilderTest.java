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


package org.apache.axis2.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.SOAPMessageFormatter;
import org.codehaus.jettison.json.JSONException;
import org.xml.sax.SAXException;

public class JSONOMBuilderTest extends TestCase {
    public void testBadgerfishQName() throws Exception {
        String jsonString = getBadgerfishJSONString();
        ByteArrayInputStream inStream = new ByteArrayInputStream(jsonString.getBytes("utf-8"));

        MessageContext msgCtx = new MessageContext();
        Builder builder = new JSONBadgerfishOMBuilder();
        OMElement elem = builder.processDocument(inStream,
                JSONTestConstants.CONTENT_TYPE_BADGERFISH, msgCtx);
        
        QName qname = elem.getQName();
        assertEquals("http://def.ns", qname.getNamespaceURI());
        assertEquals("p", qname.getLocalPart());
        assertEquals("", qname.getPrefix());
    }

    public void testBadgerfishOMSerialization1() throws IOException {

        String jsonString = getBadgerfishJSONString();
        ByteArrayInputStream inStream = new ByteArrayInputStream(jsonString.getBytes());

        MessageContext msgCtx = new MessageContext();
        Builder omBuilder = new JSONBadgerfishOMBuilder();
        OMElement elem = omBuilder.processDocument(inStream,
                JSONTestConstants.CONTENT_TYPE_BADGERFISH, msgCtx);

        elem.toString();

        SOAPEnvelope envelope = TransportUtils.createSOAPEnvelope(elem);

        msgCtx.setEnvelope(envelope);

        OMOutputFormat outputFormat = new OMOutputFormat();
        outputFormat.setCharSetEncoding(MessageContext.DEFAULT_CHAR_SET_ENCODING);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        SOAPMessageFormatter formatter = new SOAPMessageFormatter();
        formatter.writeTo(msgCtx, outputFormat, outStream, true);

        outStream.flush();
        outStream.close();
    }

    public void testBadgerfishOMSerialization2() throws XMLStreamException, JSONException,
            IOException, ParserConfigurationException, SAXException {
        String jsonString = getBadgerfishJSONString();
        ByteArrayInputStream inStream = new ByteArrayInputStream(jsonString.getBytes());
        MessageContext msgCtx = new MessageContext();

        Builder omBuilder = new JSONBadgerfishOMBuilder();
        OMElement elem = omBuilder.processDocument(inStream,
                JSONTestConstants.CONTENT_TYPE_BADGERFISH, msgCtx);

        elem.toString();

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        elem.serialize(outStream);
        outStream.flush();
        outStream.close();

    }

    public void testEmptyJsonString() throws AxisFault {
        String emptyJson = "{}";
        ByteArrayInputStream inStream = new ByteArrayInputStream(emptyJson.getBytes());
        MessageContext messageContext = new MessageContext();

        JSONOMBuilder omBuilder = new JSONOMBuilder();
        OMElement elem = omBuilder.processDocument(inStream,
                JSONTestConstants.CONTENT_TYPE_BADGERFISH, messageContext);
        // TODO: not sure why we would want to send an empty list...
//        assertEquals(null ,elem);
    }

    private String getBadgerfishJSONString() {
        return "{\"p\":{\"@xmlns\":{\"bb\":\"http://other.nsb\",\"aa\":\"http://other.ns\",\"$\":\"http://def.ns\"},\"sam\":{\"$\":\"555\", \"@att\":\"lets\"}}}";
    }

}
