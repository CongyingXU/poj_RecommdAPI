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

package org.apache.axis2.databinding.utils.reader;

import javax.activation.DataHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.ext.stax.datahandler.DataHandlerProvider;
import org.apache.axiom.ext.stax.datahandler.DataHandlerReader;
import org.apache.axiom.util.stax.XMLStreamReaderUtils;

public class WrappingXMLStreamReader implements ADBXMLStreamReader, DataHandlerReader {

    private XMLStreamReader reader;
    private DataHandlerReader dataHandlerReader;
    private int depth;
    private boolean done;

    public WrappingXMLStreamReader(XMLStreamReader reader) {
        this.reader = reader;
        dataHandlerReader = XMLStreamReaderUtils.getDataHandlerReader(reader);
    }

    public boolean isDone() {
        return done;
    }

    public Object getProperty(String string) throws IllegalArgumentException {
        return XMLStreamReaderUtils.processGetProperty(this, string);
    }

    @Override
    public boolean isBinary() {
        return dataHandlerReader != null && dataHandlerReader.isBinary();
    }

    @Override
    public boolean isOptimized() {
        return dataHandlerReader.isOptimized();
    }

    @Override
    public boolean isDeferred() {
        return dataHandlerReader.isDeferred();
    }

    @Override
    public String getContentID() {
        return dataHandlerReader.getContentID();
    }

    @Override
    public DataHandler getDataHandler() throws XMLStreamException {
        return dataHandlerReader.getDataHandler();
    }

    @Override
    public DataHandlerProvider getDataHandlerProvider() {
        return dataHandlerReader.getDataHandlerProvider();
    }

    public int next() throws XMLStreamException {
        int event = reader.next();
        if (event == START_ELEMENT) {
            depth++;
        } else if (event == END_ELEMENT) {
            depth--;
            if (depth == 0) {
                done = true;
            }
        }
        return event;
    }

    public void require(int i, String string, String string1) throws XMLStreamException {
        //nothing to do
    }

    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }

    public int nextTag() throws XMLStreamException {
        return reader.nextTag();
    }

    public boolean hasNext() throws XMLStreamException {
        return reader.hasNext();
    }

    public void close() throws XMLStreamException {
        reader.close();
    }

    public String getNamespaceURI(String string) {
        return reader.getNamespaceURI(string);
    }

    public boolean isStartElement() {
        return reader.isStartElement();
    }

    public boolean isEndElement() {
        return reader.isEndElement();
    }

    public boolean isCharacters() {
        return reader.isCharacters();
    }

    public boolean isWhiteSpace() {
        return reader.isWhiteSpace();
    }

    public String getAttributeValue(String string, String string1) {
        return reader.getAttributeValue(string, string1);
    }

    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    public QName getAttributeName(int i) {
        return reader.getAttributeName(i);
    }

    public String getAttributeNamespace(int i) {
        return reader.getAttributeNamespace(i);
    }

    public String getAttributeLocalName(int i) {
        return reader.getAttributeLocalName(i);
    }

    public String getAttributePrefix(int i) {
        return reader.getAttributePrefix(i);
    }

    public String getAttributeType(int i) {
        return reader.getAttributeType(i);
    }

    public String getAttributeValue(int i) {
        return reader.getAttributeValue(i);
    }

    public boolean isAttributeSpecified(int i) {
        return reader.isAttributeSpecified(i);
    }

    public int getNamespaceCount() {
        return reader.getNamespaceCount();
    }

    public String getNamespacePrefix(int i) {
        return reader.getNamespacePrefix(i);
    }

    public String getNamespaceURI(int i) {
        return reader.getNamespaceURI(i);
    }

    public NamespaceContext getNamespaceContext() {
        return reader.getNamespaceContext();
    }

    public int getEventType() {
        return reader.getEventType();
    }

    public String getText() {
        return reader.getText();
    }

    public char[] getTextCharacters() {
        return reader.getTextCharacters();
    }

    public int getTextCharacters(int i, char[] chars, int i1, int i2) throws XMLStreamException {
        return reader.getTextCharacters(i, chars, i1, i2);
    }

    public int getTextStart() {
        return reader.getTextStart();
    }

    public int getTextLength() {
        return reader.getTextLength();
    }

    public String getEncoding() {
        return reader.getEncoding();
    }

    public boolean hasText() {
        return reader.hasText();
    }

    public Location getLocation() {
        return reader.getLocation();
    }

    public QName getName() {
        return reader.getName();
    }

    public String getLocalName() {
        return reader.getLocalName();
    }

    public boolean hasName() {
        return reader.hasName();
    }

    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    public String getPrefix() {
        return reader.getPrefix();
    }

    public String getVersion() {
        return reader.getVersion();
    }

    public boolean isStandalone() {
        return reader.isStandalone();
    }

    public boolean standaloneSet() {
        return reader.standaloneSet();
    }

    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    public String getPITarget() {
        return reader.getPITarget();
    }

    public String getPIData() {
        return reader.getPIData();
    }

    public void addNamespaceContext(NamespaceContext nsContext) {
        //nothing to do here
    }

    public void init() {
        //Nothing to do here
    }
}
