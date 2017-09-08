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

package org.apache.axis2.databinding.typemapping;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.databinding.types.HexBinary;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.w3c.dom.Document;

import javax.activation.DataHandler;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;

public class SimpleTypeMapper {

    private static final String STRING = "java.lang.String";
    private static final String W_INT = "java.lang.Integer";
    private static final String W_DOUBLE = "java.lang.Double";
    private static final String W_LONG = "java.lang.Long";
    private static final String W_BYTE = "java.lang.Byte";
    private static final String W_SHORT = "java.lang.Short";
    private static final String W_BOOLEAN = "java.lang.Boolean";
    private static final String W_CHAR = "java.lang.Character";
    private static final String W_FLOAT = "java.lang.Float";
    private static final String W_CALENDAR = "java.util.Calendar";
    private static final String W_DATE = "java.util.Date";
    private static final String W_SQL_DATE = "java.sql.Date";
    private static final String W_URI = URI.class.getName();
    private static final String INT = "int";
    private static final String BOOLEAN = "boolean";
    private static final String BYTE = "byte";
    private static final String DOUBLE = "double";
    private static final String SHORT = "short";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String CHAR = "char";

    /*
     * To support deserialize BigDecimal, BigInteger
     * Day, Duration, Month, MonthDay, Time, Year, YearMonth
     */
    private static final String BIG_DECIMAL = "java.math.BigDecimal";
    private static final String BIG_INTEGER = "java.math.BigInteger";
    private static final String DAY = "org.apache.axis2.databinding.types.Day";
    private static final String DURATION = "org.apache.axis2.databinding.types.Duration";
    private static final String MONTH = "org.apache.axis2.databinding.types.Month";
    private static final String MONTH_DAY = "org.apache.axis2.databinding.types.MonthDay";
    private static final String TIME = "org.apache.axis2.databinding.types.Time";
    private static final String YEAR = "org.apache.axis2.databinding.types.Year";
    private static final String YEAR_MONTH = "org.apache.axis2.databinding.types.YearMonth";
    
    private static final String SQL_DATE_FORMAT = "yyyy-MM-dd";

    public static Object getSimpleTypeObject(Class parameter, String text) {
        String name = parameter.getName();

        if(name.equals(STRING)) {
            return text;
        } else  if (text == null || text.length() == 0) {
            return null;
        } else if (name.equals(INT)) {
            return new Integer(text);
        } else if (name.equals(BOOLEAN)) {
            return ConverterUtil.convertToBoolean(text);
        } else if (name.equals(BYTE)) {
            return new Byte(text);
        } else if (name.equals(DOUBLE)) {
            return new Double(text);
        } else if (name.equals(SHORT)) {
            return new Short(text);
        } else if (name.equals(LONG)) {
            return new Long(text);
        } else if (name.equals(FLOAT)) {
            return new Float(text);
        } else if (name.equals(CHAR)) {
            return text.toCharArray()[0];
        } else if (name.equals(W_INT)) {
            return new Integer(text);
        } else if (name.equals(W_BOOLEAN)) {
            return Boolean.valueOf(text);
        } else if (name.equals(W_BYTE)) {
            return new Byte(text);
        } else if (name.equals(W_DOUBLE)) {
            return new Double(text);
        } else if (name.equals(W_SHORT)) {
            return new Short(text);
        } else if (name.equals(W_LONG)) {
            return new Long(text);
        } else if (name.equals(W_FLOAT)) {
            return new Float(text);
        } else if (name.equals(W_CHAR)) {
            return text.toCharArray()[0];
        } else if (name.equals(W_CALENDAR)) {
            return makeCalendar(text);
        } else if (name.equals(W_DATE)) {
            return makeDate(text);
        } else if(name.equals(W_SQL_DATE)) {
            java.util.Date utilDate = (java.util.Date)makeDate(text);
            DateFormat sqlDateFormatter = new SimpleDateFormat(SQL_DATE_FORMAT);
            return java.sql.Date.valueOf(sqlDateFormatter.format(utilDate));            
        }
        /*
         * return the correpsonding object for adding data type
         */
        else if(name.equals(BIG_DECIMAL)) {
        	return new java.math.BigDecimal(text);
        }
        else if(name.equals(BIG_INTEGER)) {
        	return new java.math.BigInteger(text);
        }
        else if(name.equals(DAY)) {
        	return new org.apache.axis2.databinding.types.Day(text);
        }
        else if(name.equals(DURATION)) {
        	return new org.apache.axis2.databinding.types.Duration(text);
        }
        else if(name.equals(MONTH)) {
        	return new org.apache.axis2.databinding.types.Month(text);
        }
        else if(name.equals(MONTH_DAY)) {
        	return new org.apache.axis2.databinding.types.MonthDay(text);
        }
        else if(name.equals(TIME)) {
        	return new org.apache.axis2.databinding.types.Time(text);
        }
        else if(name.equals(YEAR)) {
        	return new org.apache.axis2.databinding.types.Year(text);
        }
        else if(name.equals(YEAR_MONTH)) {
        	return new org.apache.axis2.databinding.types.YearMonth(text);
        } else if(name.equals(W_URI)) {
            try {
                return new URI(text);
            } catch (URISyntaxException e) {
                throw new RuntimeException(" Invalid URI " + text, e);
            }
        } else {
            return null;
        }
    }

    public static Object getSimpleTypeObject(Class parameter, OMElement value) {
        return getSimpleTypeObject(parameter, value.getText());
    }

    public static ArrayList getArrayList(OMElement element, String localName) {
        Iterator<OMElement> childitr = element.getChildrenWithName(new QName(localName));
        ArrayList list = new ArrayList();
        while (childitr.hasNext()) {
            Object o = childitr.next();
            list.add(o);
        }
        return list;
    }

    public static HashSet getHashSet(OMElement element, String localName) {
        Iterator<OMElement> childitr = element.getChildrenWithName(new QName(localName));
        final HashSet list = new HashSet();
        while (childitr.hasNext()) {
            OMElement o = childitr.next();
            list.add(o.getText());
        }
        return list;
    }


    
    public static DataHandler getDataHandler(OMElement element) {
        OMNode node = element.getFirstOMChild();
        if (node instanceof OMText) {
            OMText txt = (OMText)node;
            if (txt.isOptimized()) {
                return (DataHandler)txt.getDataHandler();
            } else {
                return new DataHandler(new ByteArrayDataSource(Base64Utils.decode(txt.getText())));
            }
        }
        return null;
    }    

    /**
     * Gets the DataHandler according to hexBin value.
     *
     * @param element the element
     * @param hexBin the hex bin
     * @return the DataHandler object
     */
    public static DataHandler getDataHandler(OMElement element, boolean hexBin) {
       if(hexBin){
    	   ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(
   				HexBinary.decode(element.getText()));
           return new DataHandler(byteArrayDataSource);
       }
       return getDataHandler(element);
    }


    public static ArrayList getArrayList(OMElement element) {
        Iterator childitr = element.getChildren();
        ArrayList list = new ArrayList();
        while (childitr.hasNext()) {
            Object o = childitr.next();
            list.add(o);
        }
        return list;
    }

    public static boolean isSimpleType(Object obj) {
    	if(obj == null){
    		return false;
    	}
        String objClassName = obj.getClass().getName();
        return obj instanceof Calendar || obj instanceof Date || obj instanceof XMLGregorianCalendar || isSimpleType(objClassName);
    }

    public static boolean isSimpleType(Class obj) {
        String objClassName = obj.getName();
        return isSimpleType(objClassName);
    }

    public static boolean isDataHandler(Class obj) {
       return obj.isAssignableFrom(DataHandler.class) && !obj.equals(Object.class);
    }

    public static boolean isHashSet(Class obj) {
        return java.util.HashSet.class.isAssignableFrom(obj);
    }


    public static boolean isCollection(Class obj) {
        return java.util.Collection.class.isAssignableFrom(obj);
    }

    public static boolean isSimpleType(String objClassName) {
        if (objClassName.equals(STRING)) {
            return true;
        } else if (objClassName.equals(INT)) {
            return true;
        } else if (objClassName.equals(BOOLEAN)) {
            return true;
        } else if (objClassName.equals(BYTE)) {
            return true;
        } else if (objClassName.equals(DOUBLE)) {
            return true;
        } else if (objClassName.equals(SHORT)) {
            return true;
        } else if (objClassName.equals(LONG)) {
            return true;
        } else if (objClassName.equals(FLOAT)) {
            return true;
        } else if (objClassName.equals(CHAR)) {
            return true;
        } else if (objClassName.equals(W_INT)) {
            return true;
        } else if (objClassName.equals(W_BOOLEAN)) {
            return true;
        } else if (objClassName.equals(W_BYTE)) {
            return true;
        } else if (objClassName.equals(W_DOUBLE)) {
            return true;
        } else if (objClassName.equals(W_SHORT)) {
            return true;
        } else if (objClassName.equals(W_LONG)) {
            return true;
        } else if (objClassName.equals(W_FLOAT)) {
            return true;
        } else if (objClassName.equals(W_CALENDAR)) {
            return true;
        } else if (objClassName.equals(W_DATE)) {
            return true;
        } else if (objClassName.equals(W_SQL_DATE)) {
            return true;
        } else if (objClassName.equals(W_URI)) {
            return true;
        }


        /*
         * consider BigDecimal, BigInteger, Day, Duration, Month
         * MonthDay, Time, Year, YearMonth as simple type
         */
        else return objClassName.equals(BIG_DECIMAL)
                    || objClassName.equals(BIG_INTEGER)
                    || objClassName.equals(DAY)
                    || objClassName.equals(DURATION)
                    || objClassName.equals(MONTH)
                    || objClassName.equals(MONTH_DAY)
                    || objClassName.equals(TIME)
                    || objClassName.equals(YEAR)
                    || objClassName.equals(YEAR_MONTH) || objClassName.equals(W_CHAR);
    }

    public static String getStringValue(Object obj) {
        if (obj instanceof Float ||
                obj instanceof Double) {
            double data;
            if (obj instanceof Float) {
                data = ((Float)obj).doubleValue();
            } else {
                data = (Double)obj;
            }
            if (Double.isNaN(data)) {
                return "NaN";
            } else if (data == Double.POSITIVE_INFINITY) {
                return "INF";
            } else if (data == Double.NEGATIVE_INFINITY) {
                return "-INF";
            } else {
                return obj.toString();
            }
        } else if (obj instanceof Calendar) {
            Calendar calendar = (Calendar) obj;
            return ConverterUtil.convertToString(calendar);
        } else if (obj instanceof Date) {
            SimpleDateFormat zulu = new SimpleDateFormat("yyyy-MM-dd");

            MessageContext messageContext = MessageContext.getCurrentMessageContext();
            if (messageContext != null) {
                AxisService axisServce = messageContext.getAxisService();
                // if the user has given a pirticualr timezone we use it.
                if (axisServce.getParameter("TimeZone") != null) {
                    zulu.setTimeZone(TimeZone.getTimeZone((String) axisServce.getParameter("TimeZone").getValue()));
                }
            }
            return zulu.format(obj);
        } else if (obj instanceof URI){
            return obj.toString();
        }
        return obj.toString();
    }

    public static Object makeCalendar(String source) {
        return ConverterUtil.convertToDateTime(source);
    }

    public static Object makeDate(String source) {
        return ConverterUtil.convertToDate(source);
    }
    
    
	/**
	 * Checks weather passed parameter class is java.lang.Object[] or not.
	 * 
	 * @param obj the Class type of particular object.
	 * @return true, if is object array
	 */
	public static boolean isObjectArray(Class obj) {		
		if (obj != null
				&& obj.getComponentType() != null
				&& obj.getComponentType().getName()
						.equals(Object.class.getName())) {
			return true;
		}
		return false;
	}
	
	/**
	 * Checks weather passed parameter class is java.lang.Object or not.
	 *
	 * @param obj the Class type of particular object.
	 * @return true, if is object type
	 */
	public static boolean isObjectType(Class obj) {
		if (obj.getName().equals(Object.class.getName())) {
			return true;
		}
		return false;
	}
	
	/**
	 * Checks that given classType is a W3C Document.
	 * 
	 * @param classType
	 *            the class type
	 * @return true, if is dom document
	 */
	public static boolean isDomDocument(Class classType) {
		return Document.class.isAssignableFrom(classType);
	}
	
	/**
	 * Checks weather passed parameter class is a java.util.Map 
	 *
	 * @param classType the class type
	 * @return true, if it is a map
	 */
	public static boolean isMap(Class classType){
		return java.util.Map.class.isAssignableFrom(classType);
	}

	/**
	 * Checks weather passed parameter class is a multidimensional object array.
	 *
	 * @param type the type
	 * @return true, if is multidimensional object array
	 */
	public static boolean isMultidimensionalObjectArray(Class type) {
		Class compType = type.getComponentType();
		if (compType == null) {
			return false;
		} else if (isObjectArray(compType)) {
			return true;
		} else {
			return isMultidimensionalObjectArray(compType);
		}
	}

    /*check weather passed parameter class is a java.lang.Enum
    *
    * @param classType the class type
    * @return true , if it is an Enum
    * */

    public static boolean isEnum(Class classType) {
        return java.lang.Enum.class.isAssignableFrom(classType);
    }
}
