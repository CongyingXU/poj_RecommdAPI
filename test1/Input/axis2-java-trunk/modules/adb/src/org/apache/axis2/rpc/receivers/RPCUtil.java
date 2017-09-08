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

package org.apache.axis2.rpc.receivers;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.databinding.typemapping.SimpleTypeMapper;
import org.apache.axis2.databinding.utils.BeanUtil;
import org.apache.axis2.databinding.utils.Constants;
import org.apache.axis2.databinding.utils.reader.NullXMLStreamReader;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.java2wsdl.TypeTable;
import org.apache.axis2.engine.ObjectSupplier;
import org.apache.axis2.util.StreamWrapper;

import javax.activation.DataHandler;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RPCUtil {

    
    public static void processResponse(SOAPFactory fac, Object resObject,
                                       OMElement bodyContent,
                                       OMNamespace ns,
                                       SOAPEnvelope envelope,
                                       Method method,
                                       boolean qualified,
                                       TypeTable typeTable) {
        if (resObject != null) {
            //simple type
            if (resObject instanceof OMElement) {
                OMElement result = (OMElement) resObject;
                bodyContent = fac.createOMElement(
                        method.getName() + "Response", ns);
                OMElement resWrapper;
                if (qualified) {
                    resWrapper = fac.createOMElement(Constants.RETURN_WRAPPER, ns.getNamespaceURI(),
                            ns.getPrefix());
                } else {
                    resWrapper = fac.createOMElement(Constants.RETURN_WRAPPER, null);
                }
                resWrapper.addChild(result);
                bodyContent.addChild(resWrapper);
            } else if (SimpleTypeMapper.isDomDocument(resObject.getClass())){				
				OMElement doc = BeanUtil.convertDOMtoOM(fac, resObject);
				bodyContent = fac.createOMElement(method.getName() + "Response", ns);
				OMElement child;
				if (qualified) {
					child = fac.createOMElement(Constants.RETURN_WRAPPER, ns);
				} else {
					child = fac.createOMElement(Constants.RETURN_WRAPPER, null);
				}
				child.addChild(doc);
				bodyContent.addChild(child);	
			} else if (SimpleTypeMapper.isSimpleType(resObject)) {
                bodyContent = fac.createOMElement(
                        method.getName() + "Response", ns);
                OMElement child;
                if (qualified) {
                    child = fac.createOMElement(Constants.RETURN_WRAPPER, ns);
                } else {
                    child = fac.createOMElement(Constants.RETURN_WRAPPER, null);
                }
                child.addChild(fac.createOMText(child, SimpleTypeMapper.getStringValue(resObject)));
                addInstanceTypeInfo(fac, child, method, resObject, typeTable);               
                bodyContent.addChild(child);
                
            } else if (resObject instanceof XMLGregorianCalendar) {
                bodyContent = fac.createOMElement(
                        method.getName() + "Response", ns);
                OMElement child;
                if (qualified) {
                    child = fac.createOMElement(Constants.RETURN_WRAPPER, ns);
                } else {
                    child = fac.createOMElement(Constants.RETURN_WRAPPER, null);
                }
                child.addChild(fac.createOMText(child, ((XMLGregorianCalendar)resObject).toXMLFormat()));
                addInstanceTypeInfo(fac, child, method, resObject, typeTable);               
                bodyContent.addChild(child);
                
            } else {
                bodyContent = fac.createOMElement(
                        method.getName() + "Response", ns);
                // Java Beans
                QName returnWrapper;
                if (qualified) {
                    returnWrapper = new QName(ns.getNamespaceURI(), Constants.RETURN_WRAPPER, ns.getPrefix());
                } else {
                    returnWrapper = new QName(Constants.RETURN_WRAPPER);
                }
                XMLStreamReader xr = BeanUtil.getPullParser(resObject,
                        returnWrapper, typeTable, qualified, false);
                OMXMLParserWrapper stAXOMBuilder =
                        OMXMLBuilderFactory.createStAXOMBuilder(
                                OMAbstractFactory.getOMFactory(), new StreamWrapper(xr));
                OMElement documentElement = stAXOMBuilder.getDocumentElement();
                if (documentElement != null) {
                    bodyContent.addChild(documentElement);
                }
            }
        }
        if (bodyContent != null) {
            envelope.getBody().addChild(bodyContent);
        }
    }

    public static void processObjectAsDocLitBare(SOAPFactory fac,
                                                 Object resObject,
                                                 OMElement bodyContent,
                                                 OMNamespace ns,
                                                 Method method,
                                                 SOAPEnvelope envelope,
                                                 boolean qualified,
                                                 TypeTable typeTable,
                                                 String partName) {
        if (resObject instanceof OMElement) {
            OMElement result = (OMElement) resObject;
            bodyContent = fac.createOMElement(
                    partName, ns);
            bodyContent.addChild(result);
        } else if(SimpleTypeMapper.isDomDocument(resObject.getClass())){
        	bodyContent = fac.createOMElement(
                    partName, ns);          
            bodyContent.addChild(BeanUtil.convertDOMtoOM(fac, resObject));        	
        } else if (SimpleTypeMapper.isSimpleType(resObject)) {
            bodyContent = fac.createOMElement(
                    partName, ns);
            addInstanceTypeInfo(fac, bodyContent, method, resObject, typeTable); 
            bodyContent.addChild(fac.createOMText(bodyContent,
                    SimpleTypeMapper.getStringValue(resObject)));
        } else {
            QName returnWrapper = new QName(ns.getNamespaceURI(), partName, ns.getPrefix());
            XMLStreamReader xr = BeanUtil.getPullParser(resObject,
                    returnWrapper, typeTable, qualified, true);
            OMXMLParserWrapper stAXOMBuilder =
                    OMXMLBuilderFactory.createStAXOMBuilder(
                            OMAbstractFactory.getOMFactory(), new StreamWrapper(xr));
            OMElement documentElement = stAXOMBuilder.getDocumentElement();
            if (documentElement != null) {
                envelope.getBody().addChild(documentElement);
            }
        }
        if (bodyContent != null) {
            envelope.getBody().addChild(bodyContent);
        }
    }
    
	public static void processObjectAsDocLitBare(SOAPFactory fac,
			Object resObject, OMElement bodyContent, OMNamespace ns,
			SOAPEnvelope envelope, boolean qualified, TypeTable typeTable,
			String partName) {
		processObjectAsDocLitBare(fac, resObject, bodyContent, ns, null,
				envelope, qualified, typeTable, partName);

	}

    public static Object[] processRequest(OMElement methodElement,
                                          Method method, ObjectSupplier objectSupplier, String[] parameterNames)
            throws AxisFault {
        Class[] parameters = method.getParameterTypes();
        return BeanUtil.deserialize(methodElement, parameters, objectSupplier, parameterNames, method);
    }

    public static Object[] processRequest(OMElement methodElement,
                                          Method method, ObjectSupplier objectSupplier)
            throws AxisFault {
        return processRequest(methodElement, method, objectSupplier, null);
    }

    public static Object invokeServiceClass(AxisMessage inAxisMessage,
                                            Method method,
                                            Object implObject,
                                            String messageNameSpace,
                                            OMElement methodElement,
                                            MessageContext inMessage) throws AxisFault,
            IllegalAccessException, InvocationTargetException {
        
        //POJO was not WS-I complient since it does generate an empty soap message for in comming
        //soap envelope when no input parameters are set. But now we have fixed this to include the
        // the operation name wrapper.
        // this causes problems with the clients generated with the earlier wsdls. in order to
        // keep the back word compatibility we need to add this check.

        if ((inAxisMessage.getElementQName() == null) || (methodElement == null)) {
            // method accept empty SOAPbody
            return method.invoke(implObject);
        } else {
            QName elementQName = inAxisMessage.getElementQName();
            messageNameSpace = elementQName.getNamespaceURI();
            OMNamespace namespace = methodElement.getNamespace();
            if (messageNameSpace != null) {
                if (namespace == null) {
                    throw new AxisFault("namespace mismatch require " +
                            messageNameSpace +
                            " found none");
                }
                if (!messageNameSpace.equals(namespace.getNamespaceURI())) {
                    throw new AxisFault("namespace mismatch require " +
                            messageNameSpace +
                            " found " + methodElement.getNamespace().getNamespaceURI());
                }
            } else if (namespace != null) {
                throw new AxisFault(
                        "namespace mismatch. Axis Operation expects non-namespace " +
                                "qualified element. But received a namespace qualified element");
            }
            Object[] objectArray;
            if (inAxisMessage.isWrapped()) {
                Parameter namesParameter = inMessage.getAxisService().getParameter(method.getName());
                String[] parameterNames = null;
                if (namesParameter != null){
                    parameterNames = (String[]) namesParameter.getValue();
                }
                objectArray = RPCUtil.processRequest(methodElement,
                        method, inMessage.getAxisService().getObjectSupplier(), parameterNames);
            } else {
                objectArray = RPCUtil.processRequest((OMElement) methodElement.getParent(),
                        method, inMessage.getAxisService().getObjectSupplier());
            }
            return method.invoke(implObject, objectArray);

        }
    }

    public static OMElement getResponseElement(QName resname,
                                               Object[] objs,
                                               boolean qualified,
                                               TypeTable typeTable) {
        if (qualified) {
            return BeanUtil.getOMElement(resname, objs,
                    new QName(resname.getNamespaceURI(),
                	    Constants.RETURN_WRAPPER,
                            resname.getPrefix()),
                    qualified,
                    typeTable);
        } else {
            return BeanUtil.getOMElement(resname, objs,
                    new QName(Constants.RETURN_WRAPPER), qualified,
                    typeTable);
        }
    }

    public static void processResonseAsDocLitBare(Object resObject,
                                                  AxisService service,
                                                  Method method,
                                                  SOAPEnvelope envelope,
                                                  SOAPFactory fac,
                                                  OMNamespace ns,
                                                  OMElement bodyContent,
                                                  MessageContext outMessage
    ) throws Exception {
        QName elementQName = outMessage.getAxisMessage().getElementQName();
        String partName = outMessage.getAxisMessage().getPartName();
        if (resObject == null) {
            processNullReturns(service, envelope, partName);
        } else {
            if (resObject instanceof Object[]) {
                QName resName = new QName(elementQName.getNamespaceURI(),
                        partName,
                        elementQName.getPrefix());
                OMElement bodyChild = RPCUtil.getResponseElement(resName,
                        (Object[]) resObject,
                        service.isElementFormDefault(),
                        service.getTypeTable());
                envelope.getBody().addChild(bodyChild);
            } else {
                if (resObject.getClass().isArray()) {
                    int length = Array.getLength(resObject);
                    Object objArray[];
                    if (resObject instanceof byte[]) {
                        objArray = new Object[1];
                        objArray[0] = Base64Utils.encode((byte[]) resObject);
                    } else {
                        objArray = new Object[length];
                        for (int i = 0; i < length; i++) {
                            objArray[i] = Array.get(resObject, i);
                        }
                    }

                    QName resName = new QName(elementQName.getNamespaceURI(),
                            partName,
                            elementQName.getPrefix());
                    OMElement bodyChild = RPCUtil.getResponseElement(resName,
                            objArray,
                            service.isElementFormDefault(),
                            service.getTypeTable());
                    envelope.getBody().addChild(bodyChild);
                } else {
                    if (SimpleTypeMapper.isCollection(resObject.getClass())) {
                    	QName resName = new QName(
								elementQName.getNamespaceURI(),
								method.getName() + "Response",
								elementQName.getPrefix());
						OMElement bodyChild = BeanUtil.getCollectionElement(
								fac, method.getGenericReturnType(),
								(Collection) resObject, Constants.RETURN_WRAPPER,null,
								resName, service.getTypeTable(),
								service.isElementFormDefault());
						envelope.getBody().addChild(bodyChild);
						
					} else if (SimpleTypeMapper.isMap(resObject.getClass())) {
						OMElement resElemt = fac.createOMElement(
								partName, ns);
						List<OMElement> omList = BeanUtil.getMapElement(fac,
								method.getGenericReturnType(), (Map) resObject,
								service.getTypeTable(),
								service.isElementFormDefault());					
						Iterator<OMElement> omItr = omList.iterator();
						while (omItr.hasNext()) {
							resElemt.addChild(omItr.next());
						}						
						envelope.getBody().addChild(resElemt);

					} else if (SimpleTypeMapper.isDataHandler(resObject
							.getClass())) {
                        OMElement resElemt;
                        if (service.isElementFormDefault()) {
                            resElemt = fac.createOMElement(partName, ns);
                        } else {
                            resElemt = fac.createOMElement(partName, null);
                        }
                        OMText text = fac.createOMText((DataHandler)resObject, true);
                        resElemt.addChild(text);
                        envelope.getBody().addChild(resElemt);
                    } else {
                        if (service.isElementFormDefault()) {
                            RPCUtil.processObjectAsDocLitBare(fac,
                                    resObject,
                                    bodyContent,
                                    ns,
                                    method,
                                    envelope,
                                    service.isElementFormDefault(),
                                    service.getTypeTable(),
                                    partName);
                        } else {
                            RPCUtil.processObjectAsDocLitBare(fac,
                                    resObject,
                                    bodyContent,
                                    ns,
                                    method,
                                    envelope,
                                    service.isElementFormDefault(),
                                    service.getTypeTable(),
                                    partName);
                        }
                    }
                }
            }
        }
        outMessage.setEnvelope(envelope);
    }
    
	public static void processResonseAsDocLitBare(Object resObject,
			AxisService service, SOAPEnvelope envelope, SOAPFactory fac,
			OMNamespace ns, OMElement bodyContent, MessageContext outMessage)
			throws Exception {
		processResonseAsDocLitBare(resObject, service, null, envelope, fac, ns,
				bodyContent, outMessage);

	}
	
    /**
     * This method is use to to crete the reposne when , the return value is null
     *
     * @param service  Current AxisService
     * @param envelope response envelope
     * @param partName
     */
    private static void processNullReturns(AxisService service,
                                           SOAPEnvelope envelope, String partName) {
        QName resName;
        if (service.isElementFormDefault()) {
            resName = new QName(service.getSchemaTargetNamespace(),
                    partName,
                    service.getSchemaTargetNamespacePrefix());
        } else {
            resName = new QName(partName);
        }
        XMLStreamReader xr = new NullXMLStreamReader(resName);
        StreamWrapper parser = new StreamWrapper(xr);
        OMXMLParserWrapper stAXOMBuilder =
                OMXMLBuilderFactory.createStAXOMBuilder(
                        OMAbstractFactory.getSOAP11Factory(), parser);
        envelope.getBody().addChild(stAXOMBuilder.getDocumentElement());
    }


    public static void processResponseAsDocLitWrapped(Object resObject,
                                                      AxisService service,
                                                      Method method,
                                                      SOAPEnvelope envelope,
                                                      SOAPFactory fac,
                                                      OMNamespace ns,
                                                      OMElement bodyContent,
                                                      MessageContext outMessage
    ) throws Exception {
        QName elementQName = outMessage.getAxisMessage().getElementQName();
        if (resObject == null) {
            QName resName;
            if (service.isElementFormDefault()) {
                resName = new QName(service.getSchemaTargetNamespace(),
                	Constants.RETURN_WRAPPER,
                        service.getSchemaTargetNamespacePrefix());
            } else {
                resName = new QName(Constants.RETURN_WRAPPER);
            }
            XMLStreamReader xr = new NullXMLStreamReader(resName);
            StreamWrapper parser = new StreamWrapper(xr);
            OMXMLParserWrapper stAXOMBuilder =
                    OMXMLBuilderFactory.createStAXOMBuilder(
                            OMAbstractFactory.getSOAP11Factory(), parser);
            ns = fac.createOMNamespace(service.getSchemaTargetNamespace(),
                    service.getSchemaTargetNamespacePrefix());
            OMElement bodyChild = fac.createOMElement(method.getName() + "Response", ns);
            bodyChild.addChild(stAXOMBuilder.getDocumentElement());
            envelope.getBody().addChild(bodyChild);
        } else {
            if (resObject instanceof Object[]) {

                QName resName = new QName(elementQName.getNamespaceURI(),
                        method.getName() + "Response",
                        elementQName.getPrefix());
                OMElement bodyChild = RPCUtil.getResponseElement(resName,
                        (Object[]) resObject,
                        service.isElementFormDefault(),
                        service.getTypeTable());
                envelope.getBody().addChild(bodyChild);
            } else {
                if (resObject.getClass().isArray()) {
                    int length = Array.getLength(resObject);
                    Object objArray[];
                    if (resObject instanceof byte[]) {
                        objArray = new Object[1];
                        objArray[0] = Base64Utils.encode((byte[]) resObject);
                    } else {
                        objArray = new Object[length];
                        for (int i = 0; i < length; i++) {
                            objArray[i] = Array.get(resObject, i);
                        }
                    }

                    QName resName = new QName(elementQName.getNamespaceURI(),
                            method.getName() + "Response",
                            elementQName.getPrefix());
                    OMElement bodyChild = RPCUtil.getResponseElement(resName,
                            objArray,
                            service.isElementFormDefault(),
                            service.getTypeTable());
                    envelope.getBody().addChild(bodyChild);
                } else {
                    if (SimpleTypeMapper.isCollection(resObject.getClass())) {                       
						QName resName = new QName(
								elementQName.getNamespaceURI(),
								method.getName() + "Response",
								elementQName.getPrefix());
						OMElement bodyChild = BeanUtil.getCollectionElement(
								fac, method.getGenericReturnType(),
								(Collection) resObject, Constants.RETURN_WRAPPER,null,
								resName, service.getTypeTable(),
								service.isElementFormDefault());
						envelope.getBody().addChild(bodyChild);
                    } else if (SimpleTypeMapper.isMap(resObject.getClass())){
                    	 OMElement resElemt = fac.createOMElement(method.getName() + "Response", ns);
                    	 List<OMElement> omList = BeanUtil.getMapElement(fac,method.getGenericReturnType(), (Map) resObject,service.getTypeTable(),service.isElementFormDefault());
                         OMElement returnElement;
                         if (service.isElementFormDefault()) {
                             returnElement = fac.createOMElement(Constants.RETURN_WRAPPER, ns);
                         } else {
                             returnElement = fac.createOMElement(Constants.RETURN_WRAPPER, null);
                         }
                         Iterator<OMElement> omItr = omList.iterator();
                         while(omItr.hasNext()){
                        	 returnElement.addChild(omItr.next());                        	 
                         }                         
                         resElemt.addChild(returnElement);
                         envelope.getBody().addChild(resElemt);
                    	
                    } else if (SimpleTypeMapper.isDataHandler(resObject.getClass())) {
                        OMElement resElemt = fac.createOMElement(method.getName() + "Response", ns);
                        OMText text = fac.createOMText((DataHandler)resObject, true);
                        OMElement returnElement;
                        if (service.isElementFormDefault()) {
                            returnElement = fac.createOMElement(Constants.RETURN_WRAPPER, ns);
                        } else {
                            returnElement = fac.createOMElement(Constants.RETURN_WRAPPER, null);
                        }
                        returnElement.addChild(text);
                        resElemt.addChild(returnElement);
                        envelope.getBody().addChild(resElemt);
                    }
                    else {
                        if(SimpleTypeMapper.isEnum(resObject.getClass())){
                           resObject = resObject.toString();
                        }
                        if (service.isElementFormDefault()) {
                            RPCUtil.processResponse(fac, resObject, bodyContent, ns,
                                    envelope, method,
                                    service.isElementFormDefault(),
                                    service.getTypeTable());
                        } else {
                            RPCUtil.processResponse(fac, resObject, bodyContent, ns,
                                    envelope, method,
                                    service.isElementFormDefault(),
                                    service.getTypeTable());
                        }
                    }
                }
            }
        }
        outMessage.setEnvelope(envelope);
    }
    
	private static void addInstanceTypeInfo(SOAPFactory fac, OMElement element,
			Method method, Object resObject, TypeTable typeTable) {
		Class returnType = method.getReturnType();
		// add instanceTypeInfo only if return type is java.lang.Object
		if (SimpleTypeMapper.isObjectType(returnType)) {
			BeanUtil.addInstanceTypeAttribute(fac, element, resObject,
					typeTable);
		}
	}
}
