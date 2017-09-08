/**
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
package org.apache.cxf.jaxws.spring;

import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.spring.StringBeanDefinitionParser;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.spring.ServerFactoryBeanDefinitionParser;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class NamespaceHandler extends NamespaceHandlerSupport {
    public void init() {
        registerBeanDefinitionParser("client", new JaxWsProxyFactoryBeanDefinitionParser());
        registerBeanDefinitionParser("endpoint", new EndpointDefinitionParser());
        registerBeanDefinitionParser("schemaLocation", new StringBeanDefinitionParser());

        ServerFactoryBeanDefinitionParser parser = new ServerFactoryBeanDefinitionParser();
        parser.setBeanClass(SpringServerFactoryBean.class);
        registerBeanDefinitionParser("server", parser);
    }

    @NoJSR250Annotations
    public static class SpringServerFactoryBean extends JaxWsServerFactoryBean
        implements ApplicationContextAware {

        Server server;
        public SpringServerFactoryBean() {
            super();
        }
        public SpringServerFactoryBean(JaxWsServiceFactoryBean fact) {
            super(fact);
        }
        public Server getServer() {
            return server;
        }
        public Server create() {
            if (server == null) {
                server = super.create();
            }
            return server;
        }
        public void destroy() {
            if (server != null) {
                server.destroy();
                server = null;
            }
        }
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (bus == null) {
                setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
            }
        }
    }
}
