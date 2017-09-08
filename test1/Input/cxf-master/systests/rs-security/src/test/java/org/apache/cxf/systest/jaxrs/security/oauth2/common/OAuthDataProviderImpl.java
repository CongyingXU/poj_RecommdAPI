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
package org.apache.cxf.systest.jaxrs.security.oauth2.common;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.grants.code.DefaultEHCacheCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.xml.security.utils.ClassLoaderUtils;

/**
 * Extend the DefaultEHCacheCodeDataProvider to allow refreshing of tokens
 */
public class OAuthDataProviderImpl extends DefaultEHCacheCodeDataProvider {
    private Set<String> externalClients = new HashSet<>();
    public OAuthDataProviderImpl(String servicePort) throws Exception {
        // filters/grants test client
        Client client = new Client("consumer-id", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");
        client.getAllowedGrantTypes().add("implicit");
        client.getAllowedGrantTypes().add("hybrid");
        client.getAllowedGrantTypes().add("password");
        client.getAllowedGrantTypes().add("client_credentials");
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:saml2-bearer");
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:jwt-bearer");

        client.getRegisteredScopes().add("read_balance");
        client.getRegisteredScopes().add("create_balance");
        client.getRegisteredScopes().add("read_data");
        client.getRegisteredScopes().add("read_book");
        client.getRegisteredScopes().add("create_book");
        client.getRegisteredScopes().add("create_image");
        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // OIDC filters test client
        client = new Client("consumer-id-oidc", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("https://localhost:" + servicePort
                                                         + "/secured/bookstore/books"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");

        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // Audience test client
        client = new Client("consumer-id-aud", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");

        client.getRegisteredAudiences().add("https://localhost:" + servicePort
                                            + "/secured/bookstore/books");
        client.getRegisteredAudiences().add("https://127.0.0.1/test");
        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // Audience test client 2
        client = new Client("consumer-id-aud2", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");

        client.getRegisteredAudiences().add("https://localhost:" + servicePort
                                            + "/securedxyz/bookstore/books");
        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // JAXRSOAuth2Test clients
        client = new Client("alice", "alice", true);
        client.getAllowedGrantTypes().add(Constants.SAML2_BEARER_GRANT);
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:jwt-bearer");
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);

        Certificate cert = loadCert();
        String encodedCert = Base64Utility.encode(cert.getEncoded());

        Client client2 = new Client("CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US",
                                    null,
                                    true,
                                    null,
                                    null);
        client2.getAllowedGrantTypes().add("custom_grant");
        client2.setApplicationCertificates(Collections.singletonList(encodedCert));
        this.setClient(client2);

        // external clients (in LDAP/etc) which can be used for client cred
        externalClients.add("bob:bobPassword");

    }

    private Certificate loadCert() throws Exception {
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", this.getClass())) {
            return CryptoUtils.loadCertificate(is, new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}, "morpit", null);
        }
    }

    @Override
    public Client getClient(String clientId) {
        Client c = super.getClient(clientId);
        if (c == null) {
            String clientSecret = super.getCurrentClientSecret();
            if (externalClients.contains(clientId + ":" + clientSecret)) {
                c = new Client(clientId, clientSecret, true);
                c.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_BASIC);
            }
        }
        return c;

    }

    @Override
    protected boolean isRefreshTokenSupported(List<String> theScopes) {
        return true;
    }

    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScopes) {
        if (requestedScopes.isEmpty()) {
            return Collections.emptyList();
        }

        List<OAuthPermission> permissions = new ArrayList<>();
        for (String requestedScope : requestedScopes) {
            if ("read_book".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("read_book");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/secured/bookstore/books/*";
                uris.add(partnerAddress);
                permission.setUris(uris);

                permissions.add(permission);
            } else if ("create_book".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("create_book");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/secured/bookstore/books/*";
                uris.add(partnerAddress);
                permission.setUris(uris);

                permissions.add(permission);
            } else if ("create_image".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("create_image");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/secured/bookstore/image/*";
                uris.add(partnerAddress);
                permission.setUris(uris);

                permissions.add(permission);
            } else if ("read_balance".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("read_balance");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/partners/balance/*";
                uris.add(partnerAddress);
                permission.setUris(uris);

                permissions.add(permission);
            } else if ("create_balance".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("create_balance");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/partners/balance/*";
                uris.add(partnerAddress);
                permission.setUris(uris);

                permissions.add(permission);
            } else if ("read_data".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("read_data");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/partners/data/*";
                uris.add(partnerAddress);
                permission.setUris(uris);

                permissions.add(permission);
            } else if ("openid".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission("openid", "Authenticate user");
                permissions.add(permission);
            } else {
                throw new OAuthServiceException("invalid_scope");
            }
        }

        return permissions;
    }
}