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

package org.apache.cxf.systest.jaxrs.reactive;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.NotFoundException;
import javax.xml.ws.Holder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookServerAsyncClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSCompletionStageTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerAsyncClient.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerAsyncClient.class, true));
        createStaticBus();
    }

    @Before
    public void setUp() throws Exception {
        String property = System.getProperty("test.delay");
        if (property != null) {
            Thread.sleep(Long.valueOf(property));
        }
    }


    @Test
    public void testGetBookAsyncStage() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books";
        WebClient wc = createWebClient(address);
        CompletionStage<Book> stage = wc.path("123").rx().get(Book.class);
        Book book = stage.toCompletableFuture().join();
        assertEquals(123L, book.getId());
    }
    @Test
    public void testGetBookAsyncStageThenAcceptAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books";
        WebClient wc = createWebClient(address);
        CompletionStage<Book> stage = wc.path("123").rx().get(Book.class);
        Holder<Book> holder = new Holder<Book>();
        stage.thenApply(v -> {
            v.setId(v.getId() * 2);
            return v;
        }).thenAcceptAsync(v -> {
            holder.value = v;
        });
        Thread.sleep(3000);
        assertEquals(246L, holder.value.getId());
    }

    @Test
    public void testGetBookAsyncStage404() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        CompletionStage<Book> stage = wc.path("123").rx().get(Book.class);
        try {
            stage.toCompletableFuture().get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof NotFoundException);
        }

    }
    private WebClient createWebClient(String address) {
        return WebClient.create(address);
    }

}
