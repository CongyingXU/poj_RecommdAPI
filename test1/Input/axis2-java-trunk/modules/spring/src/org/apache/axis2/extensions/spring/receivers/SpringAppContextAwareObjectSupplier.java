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

package org.apache.axis2.extensions.spring.receivers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.ServiceObjectSupplier;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.extensions.spring.util.ApplicationContextUtil;
import org.apache.axis2.i18n.Messages;
import org.springframework.context.support.GenericApplicationContext;

public class SpringAppContextAwareObjectSupplier implements ServiceObjectSupplier {

    public static final String SERVICE_SPRING_BEANNAME = "SpringBeanName";

    /**
     * Method getServiceObject used to get the spring beans from the spring application context
     * for the given spring service
     *
     * @param axisService - spring service
     * @return Object
     * @throws AxisFault
     */
    public Object getServiceObject(AxisService axisService) throws AxisFault {
        try {
            // Get the Spring Context based on service, the context is set as parameter to the
            // spring service in SpringServiceDeployer when service deployed.
            GenericApplicationContext aCtx = ApplicationContextUtil.
                    getSpringApplicationContext(axisService);

            // Name of spring aware bean to be injected, taken from services.xml
            // via 'SERVICE_SPRING_BEANNAME ' . The Bean and its properties are pre-configured
            // as normally done in a spring type of way and subsequently loaded by Spring.
            // Axis2 just assumes that the bean is configured and is in the classloader.
            Parameter implBeanParam = axisService.getParameter(SERVICE_SPRING_BEANNAME);
            if (implBeanParam != null) {
                String beanName = ((String) implBeanParam.getValue()).trim();
                if (aCtx == null) {
                    throw new AxisFault("Axis2 Can't find Spring's ApplicationContext");
                } else if (aCtx.getBean(beanName) == null) {
                    throw new AxisFault("Axis2 Can't find Spring Bean: " + beanName);
                }
                return aCtx.getBean(beanName);
            } else {
                throw new AxisFault(
                        Messages.getMessage("paramIsNotSpecified", SERVICE_SPRING_BEANNAME));
            }
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }
    }
}

