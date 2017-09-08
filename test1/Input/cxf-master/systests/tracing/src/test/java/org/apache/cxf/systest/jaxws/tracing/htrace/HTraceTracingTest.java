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
package org.apache.cxf.systest.jaxws.tracing.htrace;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.TestSpanReceiver;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.TracerHeaders;
import org.apache.cxf.tracing.htrace.HTraceClientStartInterceptor;
import org.apache.cxf.tracing.htrace.HTraceClientStopInterceptor;
import org.apache.cxf.tracing.htrace.HTraceStartInterceptor;
import org.apache.cxf.tracing.htrace.HTraceStopInterceptor;
import org.apache.htrace.core.AlwaysSampler;
import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.SpanId;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

public class HTraceTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(HTraceTracingTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Tracer tracer = createTracer();
            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getInInterceptors().add(new HTraceStartInterceptor(Phase.PRE_INVOKE, tracer));
            sf.getOutInterceptors().add(new HTraceStopInterceptor(Phase.PRE_MARSHAL));
            sf.create();
        }
    }

    private interface Configurator {
        void configure(JaxWsProxyFactoryBean factory);
    }

    @BeforeClass
    public static void startServers() throws Exception {
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Before
    public void setUp() {
        TestSpanReceiver.clear();
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() throws MalformedURLException {
        final BookStoreService service = createJaxWsService();
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("POST /BookStore"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_SPAN_ID), nullValue());
    }

    @Test
    public void testThatNewInnerSpanIsCreated() throws MalformedURLException {
        final SpanId spanId = SpanId.fromRandom();
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put(TracerHeaders.DEFAULT_HEADER_SPAN_ID, Arrays.asList(spanId.toString()));

        final BookStoreService service = createJaxWsService(headers);
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("POST /BookStore"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_SPAN_ID), hasItems(spanId.toString()));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() throws MalformedURLException {
        final Tracer tracer = createTracer();

        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getOutInterceptors().add(new HTraceClientStartInterceptor(tracer));
                factory.getInInterceptors().add(new HTraceClientStopInterceptor());
            }
        });
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getParents().length, equalTo(1));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("POST /BookStore"));
        assertThat(TestSpanReceiver.getAllSpans().get(2).getDescription(),
            equalTo("POST http://localhost:" + PORT + "/BookStore"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_SPAN_ID), not(nullValue()));
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final Tracer tracer = createTracer();

        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getOutInterceptors().add(new HTraceClientStartInterceptor(tracer));
                factory.getInInterceptors().add(new HTraceClientStopInterceptor());
            }
        });

        try (TraceScope scope = tracer.newScope("test span")) {
            assertThat(service.getBooks().size(), equalTo(2));
            assertThat(Tracer.getCurrentSpan(), not(nullValue()));

            assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
            assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
            assertThat(TestSpanReceiver.getAllSpans().get(0).getParents().length, equalTo(1));
            assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("POST /BookStore"));
        }

        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReceiver.getAllSpans().get(2).getDescription(), equalTo("test span"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_SPAN_ID), not(nullValue()));
    }


    private BookStoreService createJaxWsService() throws MalformedURLException {
        return createJaxWsService(new HashMap<String, List<String>>());
    }

    private BookStoreService createJaxWsService(final Map<String, List<String>> headers) throws MalformedURLException {
        return createJaxWsService(headers, null);
    }

    private BookStoreService createJaxWsService(final Configurator configurator) throws MalformedURLException {
        return createJaxWsService(new HashMap<String, List<String>>(), configurator);
    }

    private BookStoreService createJaxWsService(final Map<String, List<String>> headers,
            final Configurator configurator) throws MalformedURLException {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setServiceClass(BookStoreService.class);
        factory.setAddress("http://localhost:" + PORT + "/BookStore");

        if (configurator != null) {
            configurator.configure(factory);
        }

        final BookStoreService service = (BookStoreService) factory.create();
        final Client proxy = ClientProxy.getClient(service);
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);

        return service;
    }

    private Map<String, List<String>> getResponseHeaders(final BookStoreService service) {
        final Client proxy = ClientProxy.getClient(service);
        return CastUtils.cast((Map<?, ?>)proxy.getResponseContext().get(Message.PROTOCOL_HEADERS));
    }

    private static Tracer createTracer() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(Tracer.SPAN_RECEIVER_CLASSES_KEY, TestSpanReceiver.class.getName());
        properties.put(Tracer.SAMPLER_CLASSES_KEY, AlwaysSampler.class.getName());

        return new Tracer.Builder("tracer")
            .conf(HTraceConfiguration.fromMap(properties))
            .build();
    }
}
