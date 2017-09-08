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

package org.apache.axis2.jaxws.xmlhttp;

import org.apache.axis2.jaxws.provider.DataSourceImpl;
import org.apache.axis2.testutils.Axis2Server;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axiom.util.io.IOUtils;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class DispatchXMessageDataSourceTests {
    @ClassRule
    public static Axis2Server server = new Axis2Server("target/repo");
    
    private QName SERVICE_NAME  = new QName("http://ws.apache.org/axis2", "XMessageDataSourceProvider");
    private QName PORT_NAME  = new QName("http://ws.apache.org/axis2", "XMessageDataSourceProviderPort");
 
    private DataSource imageDS;
    private FileDataSource txtDS;
    private DataSource attachmentDS;

    @Before
    public void setUp() throws Exception {
        String imageResourceDir = System.getProperty("basedir",".")+"/"+"test-resources"+File.separator+"image";

        File file = new File(imageResourceDir+File.separator+"test.jpg");
        ImageInputStream fiis = new FileImageInputStream(file);
        Image image = ImageIO.read(fiis);
        imageDS = new DataSourceImpl("image/jpeg","test.jpg",image);

        String textResourceDir = System.getProperty("basedir",".")+"/"+"test/org/apache/axis2/jaxws/xmlhttp";
        File file2 = new File(textResourceDir+File.separator+"README.txt");
        txtDS = new FileDataSource(file2) {
            @Override
            public String getContentType() {
                return "text/plain";
            }
        };

        String resourceDir = System.getProperty("basedir",".")+"/"+"test-resources";
        File file3 = new File(resourceDir+File.separator+"log4j.properties");
        attachmentDS = new FileDataSource(file3);
    }
    
    public Dispatch<DataSource> getDispatch() {
       Service service = Service.create(SERVICE_NAME);
       service.addPort(PORT_NAME, HTTPBinding.HTTP_BINDING, "http://localhost:" + server.getPort() + "/axis2/services/XMessageDataSourceProvider.XMessageDataSourceProviderPort");
       Dispatch<DataSource> dispatch = service.createDispatch(PORT_NAME, DataSource.class, Service.Mode.MESSAGE);
       return dispatch;
    }
    
    @Test
    public void testDataSourceWithTXT() throws Exception {
        Dispatch<DataSource> dispatch = getDispatch();
        DataSource request = txtDS;
        DataSource response = dispatch.invoke(request);
        assertTrue(response != null);
        assertThat(response.getContentType()).isEqualTo("text/plain");
        String req = new String(getStreamAsByteArray(request.getInputStream()));
        String res = new String(getStreamAsByteArray(response.getInputStream()));
        assertEquals(req, res);
    }

    @Test
    public void testDataSourceWithImage() throws Exception {
        Dispatch<DataSource> dispatch = getDispatch();
        DataSource request = imageDS;
        DataSource response = dispatch.invoke(request);
        assertTrue(response != null);
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertTrue(Arrays.equals(getStreamAsByteArray(request.getInputStream()), 
                getStreamAsByteArray(response.getInputStream())));
    }

    @Test
    public void testDataSourceWithTXTPlusAttachment() throws Exception {
        Dispatch<DataSource> dispatch = getDispatch();

        Map attachments = new HashMap();
        Map requestContext = dispatch.getRequestContext();

//        requestContext.put(org.apache.axis2.transport.http.HTTPConstants.SO_TIMEOUT , new 
//        Integer(999999));
//        requestContext.put(org.apache.axis2.transport.http.HTTPConstants.CONNECTION_TIMEOUT, new 
//        Integer(999999));

        requestContext.put(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS,
                attachments);
        attachments.put(UIDGenerator.generateContentId(), new DataHandler(attachmentDS));

        DataSource request = txtDS;
        DataSource response = dispatch.invoke(request);
        assertTrue(response != null);
        assertThat(response.getContentType()).isEqualTo("text/plain");
        String req = new String(getStreamAsByteArray(request.getInputStream()));
        String res = new String(getStreamAsByteArray(response.getInputStream()));
        assertEquals(req, res);
        Map attachments2 = (Map) dispatch.getResponseContext().get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        assertTrue(attachments2 != null);
        assertEquals(attachments2.size(), 1);
    }

    @Test
    public void testDataSourceWithImagePlusAttachment() throws Exception {
        Dispatch<DataSource> dispatch = getDispatch();

        Map attachments = new HashMap();
        Map requestContext = dispatch.getRequestContext();

        requestContext.put(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS,
                attachments);
        attachments.put(UIDGenerator.generateContentId(), new DataHandler(attachmentDS));
        
        DataSource request = imageDS;
        DataSource response = dispatch.invoke(request);
        assertTrue(response != null);
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertTrue(Arrays.equals(getStreamAsByteArray(request.getInputStream()), 
                getStreamAsByteArray(response.getInputStream())));
        Map attachments2 = (Map) dispatch.getResponseContext().get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        assertTrue(attachments2 != null);
        assertEquals(attachments2.size(), 1);
    }

    @Test
    public void testDataSourceWithTXTPlusTwoAttachments() throws Exception {
        Dispatch<DataSource> dispatch = getDispatch();

        Map attachments = new HashMap();
        Map requestContext = dispatch.getRequestContext();

        requestContext.put(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS,
                attachments);
        attachments.put(UIDGenerator.generateContentId(), new DataHandler(attachmentDS));
        attachments.put(UIDGenerator.generateContentId(), new DataHandler(imageDS));

        DataSource request = txtDS;
        DataSource response = dispatch.invoke(request);
        assertTrue(response != null);
        assertThat(response.getContentType()).isEqualTo("text/plain");
        String req = new String(getStreamAsByteArray(request.getInputStream()));
        String res = new String(getStreamAsByteArray(response.getInputStream()));
        assertEquals(req, res);
        Map attachments2 = (Map) dispatch.getResponseContext().get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        assertTrue(attachments2 != null);
        assertEquals(attachments2.size(), 2);
    }
    
    private byte[] getStreamAsByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
        IOUtils.copy(is, baos, -1);
        baos.flush();
        return baos.toByteArray();
    }
}
