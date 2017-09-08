/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdfsproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.HostsFileReader;


/**
 * Proxy Utility .
 */
public class ProxyUtil {
  public static final Log LOG = LogFactory.getLog(ProxyUtil.class);
  
  private static enum UtilityOption {
    RELOAD("-reloadPermFiles"), CLEAR("-clearUgiCache");

    private String name = null;

    private UtilityOption(String arg) {
      this.name = arg;
    }

    public String getName() {
      return name;
    }
  }
  
  /**
   * Dummy hostname verifier that is used to bypass hostname checking
   */
  private static class DummyHostnameVerifier implements HostnameVerifier {
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  }

  private static HttpsURLConnection openConnection(String hostname, int port,
      String path) throws IOException {
    try {
      final URL url = new URI("https", null, hostname, port, path, null, null)
          .toURL();
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      // bypass hostname verification
      conn.setHostnameVerifier(new DummyHostnameVerifier());
      conn.setRequestMethod("GET");
      return conn;
    } catch (URISyntaxException e) {
      throw (IOException) new IOException().initCause(e);
    }
  }

  private static void setupSslProps(Configuration conf) {
    System.setProperty("javax.net.ssl.trustStore", conf
        .get("ssl.client.truststore.location"));
    System.setProperty("javax.net.ssl.trustStorePassword", conf.get(
        "ssl.client.truststore.password", ""));
    System.setProperty("javax.net.ssl.trustStoreType", conf.get(
        "ssl.client.truststore.type", "jks"));
    System.setProperty("javax.net.ssl.keyStore", conf
        .get("ssl.client.keystore.location"));
    System.setProperty("javax.net.ssl.keyStorePassword", conf.get(
        "ssl.client.keystore.password", ""));
    System.setProperty("javax.net.ssl.keyPassword", conf.get(
        "ssl.client.keystore.keypassword", ""));
    System.setProperty("javax.net.ssl.keyStoreType", conf.get(
        "ssl.client.keystore.type", "jks"));
  }

  static InetSocketAddress getSslAddr(Configuration conf) throws IOException {
    String addr = conf.get("hdfsproxy.https.address");
    if (addr == null)
      throw new IOException("HdfsProxy address is not specified");
    return NetUtils.createSocketAddr(addr);
  }

  static boolean sendCommand(Configuration conf, String path)
      throws IOException {
    setupSslProps(conf);
    int sslPort = getSslAddr(conf).getPort();
    int err = 0;
    StringBuilder b = new StringBuilder();

    HostsFileReader hostsReader = new HostsFileReader(conf.get("hdfsproxy.hosts",
        "hdfsproxy-hosts"), "");
    Set<String> hostsList = hostsReader.getHosts();
    for (String hostname : hostsList) {
      HttpsURLConnection connection = null;
      try {
        connection = openConnection(hostname, sslPort, path);  
        connection.connect(); 
        if (LOG.isDebugEnabled()) {
          StringBuffer sb = new StringBuffer();
          X509Certificate[] clientCerts = (X509Certificate[]) connection.getLocalCertificates();
          if (clientCerts != null) {
            for (X509Certificate cert : clientCerts)
              sb.append("\n Client certificate Subject Name is "
                  + cert.getSubjectX500Principal().getName());
          } else {
            sb.append("\n No Client certs was found");  
          }
          X509Certificate[] serverCerts = (X509Certificate[]) connection.getServerCertificates();
          if (serverCerts != null) {
            for (X509Certificate cert : serverCerts)
              sb.append("\n Server certificate Subject Name is "
                  + cert.getSubjectX500Principal().getName());
          } else {
            sb.append("\n No Server certs was found");  
          }
          LOG.debug(sb.toString());
        }
        if (connection.getResponseCode() != HttpServletResponse.SC_OK) {
          b.append("\n\t" + hostname + ": " + connection.getResponseCode()
              + " " + connection.getResponseMessage());
          err++;
        }
      } catch (IOException e) {
        b.append("\n\t" + hostname + ": " + e.getLocalizedMessage());
        if (LOG.isDebugEnabled()) e.printStackTrace();
        err++;
      } finally {
        if (connection != null)
          connection.disconnect();
      }
    }
    if (err > 0) {
      System.err.print("Command failed on the following "
          + err + " host" + (err==1?":":"s:") + b.toString() + "\n");
      return false;
    }
    return true;
  }

  public static void main(String[] args) throws Exception {
    if(args.length != 1 || 
        (!UtilityOption.RELOAD.getName().equalsIgnoreCase(args[0]) 
            && !UtilityOption.CLEAR.getName().equalsIgnoreCase(args[0]))) {
      System.err.println("Usage: ProxyUtil ["
          + UtilityOption.RELOAD.getName() + "] | ["
          + UtilityOption.CLEAR.getName() + "]");
      System.exit(0);      
    }
    Configuration conf = new Configuration(false);   
    conf.addResource("ssl-client.xml");
    conf.addResource("hdfsproxy-default.xml");
     
    if (UtilityOption.RELOAD.getName().equalsIgnoreCase(args[0])) {
      // reload user-certs.xml and user-permissions.xml files
      boolean error = sendCommand(conf, "/reloadPermFiles");
    } else {
      // clear UGI caches
      boolean error = sendCommand(conf, "/clearUgiCache");
    }
  }
        
}
