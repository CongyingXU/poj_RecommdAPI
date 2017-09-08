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

package org.apache.axis2.jaxws.sample.addnumbers;

import org.apache.axis2.jaxws.TestLogger;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import java.util.List;
import java.util.Map;


@WebService(serviceName="AddNumbersService",endpointInterface="org.apache.axis2.jaxws.sample.addnumbers.AddNumbersPortType")
public class AddNumbersPortTypeImpl implements AddNumbersPortType {

    @Resource
    private WebServiceContext ctx;
    
	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.sample.addnumbers.AddNumbersPortType#addNumbers(int, int)
	 */
	public int addNumbers(int arg0, int arg1) throws AddNumbersFault_Exception {
        TestLogger.logger.debug(">> Received addNumbers request for " + arg0 + " and " + arg1);
        
        checkProperties(arg0, arg1);
        
        return arg0+arg1;
	}
	
	private void checkProperties(int arg0, int arg1) {
	    MessageContext mc = ctx.getMessageContext();
	    Map headers = (Map)mc.get(MessageContext.HTTP_REQUEST_HEADERS);
	    
	    // the map should contain some headers
	    if (headers == null || headers.isEmpty()) {
	        throw new RuntimeException("HTTP request headers map is null or empty!");
	    }
	    
	    // check for custom http headers
	    if (arg0 == 333 && arg1 == 444) {	        
	        List<String> values; 
	            
	        // test MY_HEADER_1
	        values = (List<String>) headers.get("MY_HEADER_1");
	        if (values == null || headers.isEmpty()) {
	            throw new RuntimeException("No values for MY_HEADER_1 HTTP header");
	        }
	        if (!values.contains("hello")) {
	            throw new RuntimeException("MY_HEADER_1 HTTP header does not contain expected value: " + values);
	        }

            // test MY_HEADER_2
            values = (List<String>) headers.get("MY_HEADER_2");
            if (values == null || headers.isEmpty()) {
                throw new RuntimeException("No values for MY_HEADER_2 HTTP header");
            }
            if (!values.contains("value1") && !values.contains("value2")) {
                throw new RuntimeException("MY_HEADER_2 HTTP header does not contain expected values: " + values);
            }
	    }
	}

	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.sample.addnumbers.AddNumbersPortType#oneWayInt(int)
	 */
	public void oneWayInt(int arg0) {
        TestLogger.logger.debug(">> Received one-way request.");
        return;
	}

}
