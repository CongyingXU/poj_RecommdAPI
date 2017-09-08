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
package org.apache.cxf.rs.security.oidc.utils;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public final class OidcUtils {

    public static final String ID_TOKEN_RESPONSE_TYPE = "id_token";
    public static final String ID_TOKEN_AT_RESPONSE_TYPE = "id_token token";
    public static final String CODE_AT_RESPONSE_TYPE = "code token";
    public static final String CODE_ID_TOKEN_RESPONSE_TYPE = "code id_token";
    public static final String CODE_ID_TOKEN_AT_RESPONSE_TYPE = "code id_token token";

    public static final String ID_TOKEN = "id_token";
    public static final String OPENID_SCOPE = "openid";
    public static final String PROFILE_SCOPE = "profile";
    public static final String EMAIL_SCOPE = "email";
    public static final String ADDRESS_SCOPE = "address";
    public static final String PHONE_SCOPE = "phone";
    public static final List<String> PROFILE_CLAIMS = Arrays.asList(UserInfo.NAME_CLAIM,
                                                                    UserInfo.FAMILY_NAME_CLAIM,
                                                                    UserInfo.GIVEN_NAME_CLAIM,
                                                                    UserInfo.MIDDLE_NAME_CLAIM,
                                                                    UserInfo.NICKNAME_CLAIM,
                                                                    UserInfo.PREFERRED_USERNAME_CLAIM,
                                                                    UserInfo.PROFILE_CLAIM,
                                                                    UserInfo.PICTURE_CLAIM,
                                                                    UserInfo.WEBSITE_CLAIM,
                                                                    UserInfo.GENDER_CLAIM,
                                                                    UserInfo.BIRTHDATE_CLAIM,
                                                                    UserInfo.ZONEINFO_CLAIM,
                                                                    UserInfo.LOCALE_CLAIM,
                                                                    UserInfo.UPDATED_AT_CLAIM);
    public static final List<String> EMAIL_CLAIMS = Arrays.asList(UserInfo.EMAIL_CLAIM,
                                                                  UserInfo.EMAIL_VERIFIED_CLAIM);
    public static final List<String> ADDRESS_CLAIMS = Arrays.asList(UserInfo.ADDRESS_CLAIM);
    public static final List<String> PHONE_CLAIMS = Arrays.asList(UserInfo.PHONE_CLAIM);
    public static final String CLAIMS_PARAM = "claims";
    public static final String CLAIM_NAMES_PROPERTY = "_claim_names";
    public static final String CLAIM_SOURCES_PROPERTY = "_claim_sources";
    public static final String JWT_CLAIM_SOURCE_PROPERTY = "JWT";
    public static final String ENDPOINT_CLAIM_SOURCE_PROPERTY = "endpoint";
    public static final String TOKEN_CLAIM_SOURCE_PROPERTY = "access_token";

    public static final String PROMPT_PARAMETER = "prompt";
    public static final String PROMPT_NONE_VALUE = "none";
    public static final String PROMPT_CONSENT_VALUE = "consent";
    public static final String CONSENT_REQUIRED_ERROR = "consent_required";

    private static final Map<String, List<String>> SCOPES_MAP;
    static {
        SCOPES_MAP = new HashMap<>();
        SCOPES_MAP.put(PHONE_SCOPE, PHONE_CLAIMS);
        SCOPES_MAP.put(EMAIL_SCOPE, EMAIL_CLAIMS);
        SCOPES_MAP.put(ADDRESS_SCOPE, ADDRESS_CLAIMS);
        SCOPES_MAP.put(PROFILE_SCOPE, PROFILE_CLAIMS);
    }

    private OidcUtils() {

    }
    public static List<String> getPromptValues(MultivaluedMap<String, String> params) {
        String prompt = params.getFirst(PROMPT_PARAMETER);
        if (prompt != null) {
            return Arrays.asList(prompt.trim().split(" "));
        } else {
            return Collections.emptyList();
        }
    }

    public static String getOpenIdScope() {
        return OPENID_SCOPE;
    }
    public static String getProfileScope() {
        return getScope(OPENID_SCOPE, PROFILE_SCOPE);
    }
    public static String getEmailScope() {
        return getScope(OPENID_SCOPE, EMAIL_SCOPE);
    }
    public static String getAddressScope() {
        return getScope(OPENID_SCOPE, ADDRESS_SCOPE);
    }
    public static String getPhoneScope() {
        return getScope(OPENID_SCOPE, PHONE_SCOPE);
    }
    public static String getAllScopes() {
        return getScope(OPENID_SCOPE, PROFILE_SCOPE, EMAIL_SCOPE, ADDRESS_SCOPE, PHONE_SCOPE);
    }
    
    public static List<String> getScopeClaims(String... scope) {
        List<String> claims = new ArrayList<>();
        if (scope != null) {
            for (String s : scope) {
                if (SCOPES_MAP.containsKey(s)) {
                    claims.addAll(SCOPES_MAP.get(s));
                }
            }
        }
        return claims;
    }

    private static String getScope(String... scopes) {
        StringBuilder sb = new StringBuilder();
        for (String scope : scopes) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(scope);
        }
        return sb.toString();
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt) {
        validateAccessTokenHash(at, jwt, true);
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt, boolean required) {
        validateAccessTokenHash(at.getTokenKey(), jwt, required);
    }
    public static void validateAccessTokenHash(String accessToken, JwtToken jwt, boolean required) {
        if (required) {
            validateHash(accessToken,
                         (String)jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM),
                         jwt.getJwsHeaders().getSignatureAlgorithm());
        }
    }
    public static void validateCodeHash(String code, JwtToken jwt) {
        validateCodeHash(code, jwt, true);
    }
    public static void validateCodeHash(String code, JwtToken jwt, boolean required) {
        if (required) {
            validateHash(code,
                         (String)jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM),
                         jwt.getJwsHeaders().getSignatureAlgorithm());
        }
    }
    private static void validateHash(String value, String theHash, SignatureAlgorithm joseAlgo) {
        String hash = calculateHash(value, joseAlgo);
        if (!hash.equals(theHash)) {
            throw new OAuthServiceException("Invalid hash");
        }
    }
    public static String calculateAccessTokenHash(String value, SignatureAlgorithm sigAlgo) {
        return calculateHash(value, sigAlgo);
    }
    public static String calculateAuthorizationCodeHash(String value, SignatureAlgorithm sigAlgo) {
        return calculateHash(value, sigAlgo);
    }
    private static String calculateHash(String value, SignatureAlgorithm sigAlgo) {
        if (sigAlgo == SignatureAlgorithm.NONE) {
            throw new JwsException(JwsException.Error.INVALID_ALGORITHM);
        }
        String algoShaSizeString = sigAlgo.getJwaName().substring(2);
        String javaShaAlgo = "SHA-" + algoShaSizeString;
        int algoShaSize = Integer.valueOf(algoShaSizeString);
        int valueHashSize = (algoShaSize / 8) / 2;
        try {
            byte[] atBytes = StringUtils.toBytesASCII(value);
            byte[] digest = MessageDigestUtils.createDigest(atBytes,  javaShaAlgo);
            return Base64UrlUtility.encodeChunk(digest, 0, valueHashSize);
        } catch (NoSuchAlgorithmException ex) {
            throw new OAuthServiceException(ex);
        }
    }
    public static void setStateClaimsProperty(OAuthRedirectionState state,
                                              MultivaluedMap<String, String> params) {
        String claims = params.getFirst(OidcUtils.CLAIMS_PARAM);
        if (claims != null) {
            state.getExtraProperties().put(OidcUtils.CLAIMS_PARAM, claims);
        }
    }
}
