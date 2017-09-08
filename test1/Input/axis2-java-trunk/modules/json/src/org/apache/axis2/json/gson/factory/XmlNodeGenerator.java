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

package org.apache.axis2.json.gson.factory;

import org.apache.axis2.AxisFault;
import org.apache.ws.commons.schema.utils.XmlSchemaRef;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;

import javax.xml.namespace.QName;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class XmlNodeGenerator {

    List<XmlSchema> xmlSchemaList;

    QName elementQname;

    private XmlNode mainXmlNode;

    Queue<JsonObject> queue = new LinkedList<JsonObject>();

    public XmlNodeGenerator(List<XmlSchema> xmlSchemaList, QName elementQname) {
        this.xmlSchemaList = xmlSchemaList;
        this.elementQname = elementQname;
    }

    public XmlNodeGenerator() {
    }

    private void processSchemaList() throws AxisFault {
        // get the operation schema and process.
        XmlSchema operationSchema = getXmlSchema(elementQname);
        XmlSchemaElement messageElement = operationSchema.getElementByName(elementQname.getLocalPart());
        mainXmlNode = new XmlNode(elementQname.getLocalPart(), elementQname.getNamespaceURI() , false, (messageElement.getMaxOccurs() == 1 ? false : true) , "");

        QName messageSchemaTypeName = messageElement.getSchemaTypeName();
        XmlSchemaType schemaType = null;
        XmlSchema schemaOfType = null;
        if (messageSchemaTypeName != null) {
            schemaType = operationSchema.getTypeByName(messageSchemaTypeName);
            if (schemaType == null) {
                schemaOfType = getXmlSchema(messageSchemaTypeName);
                schemaType = schemaOfType.getTypeByName(messageSchemaTypeName.getLocalPart());
            } else {
                schemaOfType = operationSchema;
            }
        } else {
            schemaType = messageElement.getSchemaType();
            schemaOfType = operationSchema;
        }

        if (schemaType != null) {
            processSchemaType(schemaType, mainXmlNode, schemaOfType);
        } else {
            // nothing to do
        }
    }

    private void processElement(XmlSchemaElement element, XmlNode parentNode , XmlSchema schema) throws AxisFault {
        String targetNamespace = schema.getTargetNamespace();
        XmlNode xmlNode;
        QName schemaTypeName = element.getSchemaTypeName();
        XmlSchemaType schemaType = element.getSchemaType();
        if (schemaTypeName != null) {
            xmlNode = new XmlNode(element.getName(), targetNamespace, false, (element.getMaxOccurs() == 1 ? false : true), schemaTypeName.getLocalPart());
            parentNode.addChildToList(xmlNode);
            if (("http://www.w3.org/2001/XMLSchema").equals(schemaTypeName.getNamespaceURI())) {
            } else {
                XmlSchema schemaOfType;
                // see whether Schema type is in the same schema
                XmlSchemaType childSchemaType = schema.getTypeByName(schemaTypeName.getLocalPart());
                if (childSchemaType == null) {
                    schemaOfType = getXmlSchema(schemaTypeName);
                    childSchemaType = schemaOfType.getTypeByName(schemaTypeName.getLocalPart());
                }else{
                    schemaOfType = schema;
                }
                processSchemaType(childSchemaType, xmlNode, schemaOfType);
            }
        }else if (schemaType != null) {
            xmlNode = new XmlNode(element.getName(), targetNamespace, false, (element.getMaxOccurs() == 1 ? false : true), schemaType.getQName().getLocalPart());
            parentNode.addChildToList(xmlNode);
            processSchemaType(schemaType, xmlNode, schema);
        }else if (element.getRef() != null) {
            // Handle ref element
            XmlSchemaRef xmlSchemaRef = element.getRef();
            QName targetQname = xmlSchemaRef.getTargetQName();
            if (targetQname == null) {
                throw new AxisFault("target QName is null while processing ref:" + element.getName());
            }
            getXmlSchema(targetQname);
            xmlNode = new XmlNode(targetQname.getLocalPart(), targetNamespace, false, (element.getMaxOccurs() != 1), targetQname.getLocalPart());
            parentNode.addChildToList(xmlNode);
            if (("http://www.w3.org/2001/XMLSchema").equals(targetQname.getNamespaceURI())) {
            } else {
                XmlSchema schemaOfType;
                // see whether Schema type is in the same schema
                XmlSchemaType childSchemaType = schema.getTypeByName(targetQname.getLocalPart());
                if (childSchemaType == null) {
                    schemaOfType = getXmlSchema(targetQname);
                    childSchemaType = schemaOfType.getTypeByName(targetQname.getLocalPart());
                } else {
                    schemaOfType = schema;
                }
                processSchemaType(childSchemaType, xmlNode, schemaOfType);
            }
        }
    }


    private void processSchemaType(XmlSchemaType xmlSchemaType , XmlNode parentNode , XmlSchema schema) throws AxisFault {
        if (xmlSchemaType instanceof XmlSchemaComplexType) {
            XmlSchemaComplexType complexType = (XmlSchemaComplexType)xmlSchemaType;
            XmlSchemaParticle particle = complexType.getParticle();
            if (particle instanceof XmlSchemaSequence) {
                XmlSchemaSequence sequence = (XmlSchemaSequence)particle;
                for (XmlSchemaSequenceMember member : sequence.getItems()) {
                    if (member instanceof XmlSchemaElement) {
                        processElement((XmlSchemaElement)member , parentNode , schema);
                    }
                }
            }
        }else if (xmlSchemaType instanceof XmlSchemaSimpleType) {
            // nothing to do with simpleType
        }
    }


    private XmlSchema getXmlSchema(QName qName) {
        for (XmlSchema xmlSchema : xmlSchemaList) {
            if (xmlSchema.getTargetNamespace().equals(qName.getNamespaceURI())) {
                return xmlSchema;
            }
        }
        return null;
    }

    private void generateQueue(XmlNode node) {
        if (node.isArray()) {
            if (node.getChildrenList().size() > 0) {
                queue.add(new JsonObject(node.getName(), JSONType.NESTED_ARRAY, node.getValueType() , node.getNamespaceUri()));
                processXmlNodeChildren(node.getChildrenList());
            } else {
                queue.add(new JsonObject(node.getName(), JSONType.ARRAY , node.getValueType() , node.getNamespaceUri()));
            }
        } else {
            if (node.getChildrenList().size() > 0) {
                queue.add(new JsonObject(node.getName(), JSONType.NESTED_OBJECT, node.getValueType() , node.getNamespaceUri()));
                processXmlNodeChildren(node.getChildrenList());
            } else {
                queue.add(new JsonObject(node.getName(), JSONType.OBJECT , node.getValueType() , node.getNamespaceUri()));
            }
        }
    }

    private void processXmlNodeChildren(List<XmlNode> childrenNodes) {
        for (int i = 0; i < childrenNodes.size(); i++) {
            generateQueue(childrenNodes.get(i));
        }
    }


    public XmlNode getMainXmlNode() throws AxisFault {
        if (mainXmlNode == null) {
            try {
                processSchemaList();
            } catch (AxisFault axisFault) {
                throw new AxisFault("Error while creating intermeidate xml structure ", axisFault);
            }
        }
        return mainXmlNode;
    }

    public Queue<JsonObject> getQueue(XmlNode node) {
        generateQueue(node);
        return queue;
    }

}
