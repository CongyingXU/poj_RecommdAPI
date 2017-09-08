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

package org.apache.axis2.jaxws.message.databinding.impl;

import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.datasource.jaxb.JAXBDSContext;
import org.apache.axis2.datasource.jaxb.JAXBDataSource;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.message.databinding.JAXBBlock;
import org.apache.axis2.jaxws.message.databinding.JAXBBlockContext;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.impl.BlockImpl;
import org.apache.axis2.jaxws.message.util.XMLStreamWriterWithOS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * JAXBBlockImpl <p/> A Block containing a JAXB business object (either a JAXBElement or an object
 * with @XmlRootElement).
 */
public class JAXBBlockImpl extends BlockImpl<Object,JAXBBlockContext> implements JAXBBlock {

    private static final Log log = LogFactory.getLog(JAXBBlockImpl.class);

    private static final boolean DEBUG_ENABLED = log.isDebugEnabled();

    /**
     * Called by JAXBBlockFactory
     * 
     * @param busObject..The business object must be a JAXBElement or an object with an
     * @XMLRootElement. This is assertion is validated in the JAXBFactory.
     * @param busContext
     * @param qName QName must be non-null
     * @param factory
     */
    JAXBBlockImpl(Object busObject, JAXBBlockContext busContext, QName qName, 
                  BlockFactory factory)
            throws JAXBException {
        super(busObject, busContext, qName, factory);
    }

    /**
     * Called by JAXBBlockFactory
     * 
     * @param omelement
     * @param busContext
     * @param qName must be non-null
     * @param factory
     */
    JAXBBlockImpl(OMElement omElement, JAXBBlockContext busContext, QName qName,
            BlockFactory factory) {
        super(omElement, busContext, qName, factory);
    }

    @Override
    protected Object _getBOFromOM(OMElement omElement, JAXBBlockContext busContext)
        throws XMLStreamException, WebServiceException {
        
        // Shortcut to get business object from existing data source
        if (omElement instanceof OMSourcedElement) {
            OMDataSource ds = ((OMSourcedElement) omElement).getDataSource();
            if (ds instanceof JAXBDataSource) {
                // Update the business context to use the one provided
                // by the datasource
                try {
                    JAXBDSContext dsContext = ((JAXBDataSource) ds).getContext();
                    busContext = new JAXBBlockContext(dsContext.getJAXBContext());
                } catch (JAXBException e) {
                    throw ExceptionFactory.makeWebServiceException(e);
                }
                return ((JAXBDataSource) ds).getObject();
            } else if (ds instanceof JAXBBlockImpl) {
                // Update the business context to use the one provided by the
                // by the datasource
                JAXBBlockContext blockContext = (JAXBBlockContext) ((JAXBBlockImpl) ds).getBusinessContext();
                busContext = blockContext;

                return ((JAXBBlockImpl) ds).getObject();
            }
        }
        
        try {
            return busContext.unmarshal(omElement);
        } catch (JAXBException je) {
            if (DEBUG_ENABLED) {
                try {
                    log.debug("JAXBContext for unmarshal failure:" + 
                              busContext.getJAXBContext(busContext.getClassLoader()));
                } catch (Exception e) {
                }
            }
            throw ExceptionFactory.makeWebServiceException(je);
        }
    }

    /**
     * @param busObj
     * @param busContext
     * @return
     * @throws XMLStreamException
     * @throws WebServiceException
     */
    private byte[] _getBytesFromBO(Object busObj, JAXBBlockContext busContext, String encoding)
        throws XMLStreamException, WebServiceException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Exposes getOutputStream, which allows faster writes.
        XMLStreamWriterWithOS writer = new XMLStreamWriterWithOS(baos, encoding);

        // Write the business object to the writer
        _outputFromBO(busObj, busContext, writer);

        // Flush the writer
        writer.flush();
        writer.close();
        return baos.toByteArray();
    }


    @Override
    protected XMLStreamReader _getReaderFromBO(Object busObj, JAXBBlockContext busContext)
        throws XMLStreamException, WebServiceException {
        ByteArrayInputStream baos =
                new ByteArrayInputStream(_getBytesFromBO(busObj, busContext, "utf-8"));
        return StAXUtils.createXMLStreamReader(baos, "utf-8");
    }
    
    @Override
    protected void _outputFromBO(Object busObject, JAXBBlockContext busContext, XMLStreamWriter writer)
        throws XMLStreamException, WebServiceException {
        try {
            busContext.marshal(busObject, writer);
        } catch (JAXBException je) {
            if (DEBUG_ENABLED) {
                try {
                    log.debug("JAXBContext for marshal failure:" + 
                              busContext.getJAXBContext(busContext.getClassLoader()));
                } catch (Exception e) {
                }
            }
            throw ExceptionFactory.makeWebServiceException(je);
        }
    }

    @Override
    public boolean isElementData() {
        return true;
    }
    
    @Override
    public void close() {
        return; // Nothing to close
    }

    @Override
    public Object getObject() {
        try {
            return getBusinessObject(false);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    @Override
    public boolean isDestructiveRead() {
        return false;
    }

    @Override
    public boolean isDestructiveWrite() {
        return false;
    }
    
    @Override
    public OMDataSourceExt copy() throws OMException {
        
        if (DEBUG_ENABLED) {
            log.debug("Making a copy of the JAXB object");
        }
        return new JAXBDataSource(this.getObject(), 
                                  (JAXBDSContext) this.getBusinessContext());
    }

    @Override
    public void setParent(Message message) {
        if (busContext != null) {
            busContext.setMessage(message);
        }
        super.setParent(message);
    }

    

}
