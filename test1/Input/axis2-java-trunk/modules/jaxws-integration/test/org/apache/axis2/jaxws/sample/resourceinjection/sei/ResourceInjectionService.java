
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

package org.apache.axis2.jaxws.sample.resourceinjection.sei;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@WebServiceClient(name = "ResourceInjectionService", targetNamespace = "http://resourceinjection.sample.jaxws.axis2.apache.org", wsdlLocation = "resourceinjection.wsdl")
public class ResourceInjectionService
    extends Service
{

    private final static URL RESOURCEINJECTIONSERVICE_WSDL_LOCATION;

    private static String wsdlLocation="/test/org/apache/axis2/jaxws/sample/resourceinjection/META-INF/resourceinjection.wsdl";
    static {
        URL url = null;
        try {
            try{
                String baseDir = new File(System.getProperty("basedir",".")).getCanonicalPath();
                wsdlLocation = new File(baseDir + wsdlLocation).getAbsolutePath();
            }catch(Exception e){
                e.printStackTrace();
            }
            File file = new File(wsdlLocation);
            url = file.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        RESOURCEINJECTIONSERVICE_WSDL_LOCATION = url;
    }

    public ResourceInjectionService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ResourceInjectionService() {
        super(RESOURCEINJECTIONSERVICE_WSDL_LOCATION, new QName("http://resourceinjection.sample.jaxws.axis2.apache.org", "ResourceInjectionService"));
    }

    /**
     * 
     * @return
     *     returns ResourceInjectionPortType
     */
    @WebEndpoint(name = "ResourceInjectionPort")
    public ResourceInjectionPortType getResourceInjectionPort() {
        return (ResourceInjectionPortType)super.getPort(new QName("http://resourceinjection.sample.jaxws.axis2.apache.org", "ResourceInjectionPort"), ResourceInjectionPortType.class);
    }

}
