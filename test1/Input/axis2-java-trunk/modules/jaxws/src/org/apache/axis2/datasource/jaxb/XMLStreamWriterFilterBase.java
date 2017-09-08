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
package org.apache.axis2.datasource.jaxb;

import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;

/**
 * The base class for classes that are MTOMXMLStreamWriter filters.
 * Each of the XMLStreamWriter events is intercepted and passed to the delegate XMLStreamWriter
 * 
 * Character data is sent to the xmlData abstract method.  Derived classes may 
 * log or change the xml data.
 * 
 * @see XMLStreamWriterRemoveIllegalChars
 */
public abstract class XMLStreamWriterFilterBase extends MTOMXMLStreamWriter {

    private final MTOMXMLStreamWriter delegate;

    public XMLStreamWriterFilterBase(MTOMXMLStreamWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context)
            throws XMLStreamException {
        delegate.setNamespaceContext(context);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI,
            String localName, String value) throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, xmlData(value));
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName,
            String value) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, xmlData(value));
    }

    @Override
    public void writeAttribute(String localName, String value)
            throws XMLStreamException {
        delegate.writeAttribute(localName, xmlData(value));
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        delegate.writeCData(xmlData(data));
    }

    @Override
    public void writeCharacters(char[] text, int start, int len)
            throws XMLStreamException {
        // Adapt to writeCharacters that takes a String value
        String value = new String(text, start, len);
        writeCharacters(value);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        delegate.writeCharacters(xmlData(text));
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        delegate.writeComment(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        delegate.writeDTD(dtd);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI)
            throws XMLStreamException {
        delegate.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName,
            String namespaceURI) throws XMLStreamException {
        delegate.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName)
            throws XMLStreamException {
        delegate.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        delegate.writeEmptyElement(localName);
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        delegate.writeEntityRef(name);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI)
            throws XMLStreamException {
        delegate.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeProcessingInstruction(String target, String data)
            throws XMLStreamException {
        delegate.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeProcessingInstruction(String target)
            throws XMLStreamException {
        delegate.writeProcessingInstruction(target);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String encoding, String version)
            throws XMLStreamException {
        delegate.writeStartDocument(encoding, version);
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        delegate.writeStartDocument(version);
    }

    @Override
    public void writeStartElement(String prefix, String localName,
            String namespaceURI) throws XMLStreamException {
        delegate.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName)
            throws XMLStreamException {
        delegate.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        delegate.writeStartElement(localName);
    }

    @Override
    public boolean isOptimized() {
        return delegate.isOptimized();
    }

    @Override
    public String prepareDataHandler(DataHandler dataHandler) {
        return delegate.prepareDataHandler(dataHandler);
    }

    @Override
    public String getCharSetEncoding() {
        return delegate.getCharSetEncoding();
    }

    @Override
    public OMOutputFormat getOutputFormat() {
        return delegate.getOutputFormat();
    }

    @Override
    public OutputStream getOutputStream() throws XMLStreamException {
        // Since the filter may modify the data, we can't allow access to the raw output stream
        return null;
    }

    /**
     * Derived classes extend the method.  A derived class may log or modify the xml data
     * @param value
     * @return value
     */
    protected abstract String xmlData(String value);

}
