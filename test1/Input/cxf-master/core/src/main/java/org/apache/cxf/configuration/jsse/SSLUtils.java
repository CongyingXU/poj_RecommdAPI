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

package org.apache.cxf.configuration.jsse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.security.FiltersType;


/**
 * Holder for utility methods related to manipulating SSL settings, common
 * to the connection and listener factories (previously duplicated).
 */
public final class SSLUtils {

    static final String PKCS12_TYPE = "PKCS12";

    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
    private static final String DEFAULT_TRUST_STORE_TYPE = "JKS";
    private static final String DEFAULT_SECURE_SOCKET_PROTOCOL = "TLSv1";

    private static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";

    private static final List<String> DEFAULT_CIPHERSUITE_FILTERS_INCLUDE =
        Arrays.asList(new String[] {".*"});
    /**
     * By default, exclude NULL, anon, EXPORT, DES ciphersuites
     */
    private static final List<String> DEFAULT_CIPHERSUITE_FILTERS_EXCLUDE =
        Arrays.asList(new String[] {".*_NULL_.*",
                                    ".*_anon_.*",
                                    ".*_EXPORT_.*",
                                    ".*_DES_.*"});

    private static volatile KeyManager[] defaultManagers;

    private SSLUtils() {
    }

    public static KeyManager[] getDefaultKeyStoreManagers(Logger log) {
        if (defaultManagers == null) {
            loadDefaultKeyManagers(log);
        }
        if (defaultManagers.length == 0) {
            return null;
        }
        return defaultManagers;
    }

    private static synchronized void loadDefaultKeyManagers(Logger log) {
        if (defaultManagers != null) {
            return;
        }

        String location = getKeystore(null, log);
        String keyStorePassword = getKeystorePassword(null, log);
        String keyPassword = getKeyPassword(null, log);
        InputStream is = null;

        try {
            File file = new File(location);
            if (file.exists()) {
                KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

                is = Files.newInputStream(file.toPath());
                ks.load(is, (keyStorePassword != null) ? keyStorePassword.toCharArray() : null);
                kmf.init(ks, (keyPassword != null) ? keyPassword.toCharArray() : null);
                defaultManagers = kmf.getKeyManagers();
            } else {
                log.log(Level.FINER, "No default keystore {0}", location);
                defaultManagers = new KeyManager[0];
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Default key managers cannot be initialized: " + e.getMessage(), e);
            defaultManagers = new KeyManager[0];
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warning("Keystore stream cannot be closed: " + e.getMessage());
                }
            }
        }
    }

    public static KeyManager[] loadKeyStore(KeyManagerFactory kmf,
                                               KeyStore ks,
                                               ByteArrayInputStream bin,
                                               String keyStoreLocation,
                                               String keyStorePassword,
                                               Logger log) {
        KeyManager[] keystoreManagers = null;
        try {
            ks.load(bin, keyStorePassword.toCharArray());
            kmf.init(ks, keyStorePassword.toCharArray());
            keystoreManagers = kmf.getKeyManagers();
            LogUtils.log(log,
                         Level.FINE,
                         "LOADED_KEYSTORE",
                         keyStoreLocation);
        } catch (Exception e) {
            LogUtils.log(log,
                         Level.WARNING,
                         "FAILED_TO_LOAD_KEYSTORE",
                         new Object[]{keyStoreLocation, e.getMessage()});
        }
        return keystoreManagers;
    }

    protected static byte[] loadFile(String fileName) throws IOException {
        if (fileName == null) {
            return null;
        }
        Path path = FileSystems.getDefault().getPath(fileName);
        return Files.readAllBytes(path);
    }

    public static String getKeystore(String keyStoreLocation, Logger log) {
        String logMsg = null;
        if (keyStoreLocation != null) {
            logMsg = "KEY_STORE_SET";
        } else {
            keyStoreLocation = SystemPropertyAction.getProperty("javax.net.ssl.keyStore");
            if (keyStoreLocation != null) {
                logMsg = "KEY_STORE_SYSTEM_PROPERTY_SET";
            } else {
                keyStoreLocation =
                    SystemPropertyAction.getProperty("user.home") + "/.keystore";
                logMsg = "KEY_STORE_NOT_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreLocation);
        return keyStoreLocation;
    }

    public static String getKeystoreType(String keyStoreType, Logger log) {
        return getKeystoreType(keyStoreType, log, DEFAULT_KEYSTORE_TYPE);
    }

    public static String getKeystoreType(String keyStoreType, Logger log, String def) {
        String logMsg = null;
        if (keyStoreType != null) {
            logMsg = "KEY_STORE_TYPE_SET";
        } else {
            keyStoreType = SystemPropertyAction.getProperty("javax.net.ssl.keyStoreType", null);
            if (keyStoreType == null) {
                keyStoreType = def;
                logMsg = "KEY_STORE_TYPE_NOT_SET";
            } else {
                logMsg = "KEY_STORE_TYPE_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreType);
        return keyStoreType;
    }

    public static String getKeystoreProvider(String keyStoreProvider, Logger log) {
        String logMsg = null;
        if (keyStoreProvider != null) {
            logMsg = "KEY_STORE_PROVIDER_SET";
        } else {
            keyStoreProvider = SystemPropertyAction.getProperty("javax.net.ssl.keyStoreProvider", null);
            if (keyStoreProvider == null) {
                logMsg = "KEY_STORE_PROVIDER_NOT_SET";
            } else {
                logMsg = "KEY_STORE_PROVIDER_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreProvider);
        return keyStoreProvider;
    }

    public static String getKeystorePassword(String keyStorePassword,
                                             Logger log) {
        String logMsg = null;
        if (keyStorePassword != null) {
            logMsg = "KEY_STORE_PASSWORD_SET";
        } else {
            keyStorePassword =
                SystemPropertyAction.getProperty("javax.net.ssl.keyStorePassword");
            logMsg = keyStorePassword != null
                     ? "KEY_STORE_PASSWORD_SYSTEM_PROPERTY_SET"
                     : "KEY_STORE_PASSWORD_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg);
        return keyStorePassword;
    }

    public static String getKeyPassword(String keyPassword, Logger log) {
        String logMsg = null;
        if (keyPassword != null) {
            logMsg = "KEY_PASSWORD_SET";
        } else {
            keyPassword =
                SystemPropertyAction.getProperty("javax.net.ssl.keyPassword");
            if (keyPassword == null) {
                keyPassword =
                    SystemPropertyAction.getProperty("javax.net.ssl.keyStorePassword");
            }
            logMsg = keyPassword != null
                     ? "KEY_PASSWORD_SYSTEM_PROPERTY_SET"
                     : "KEY_PASSWORD_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg);
        return keyPassword;
    }

    public static String getKeystoreAlgorithm(
                                          String keyStoreMgrFactoryAlgorithm,
                                          Logger log) {
        String logMsg = null;
        if (keyStoreMgrFactoryAlgorithm != null) {
            logMsg = "KEY_STORE_ALGORITHM_SET";
        } else {
            keyStoreMgrFactoryAlgorithm =
                KeyManagerFactory.getDefaultAlgorithm();
            logMsg = "KEY_STORE_ALGORITHM_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreMgrFactoryAlgorithm);
        return keyStoreMgrFactoryAlgorithm;
    }

    public static String getTrustStoreAlgorithm(
                                        String trustStoreMgrFactoryAlgorithm,
                                        Logger log) {
        String logMsg = null;
        if (trustStoreMgrFactoryAlgorithm != null) {
            logMsg = "TRUST_STORE_ALGORITHM_SET";
        } else {
            trustStoreMgrFactoryAlgorithm =
                TrustManagerFactory.getDefaultAlgorithm();
            logMsg = "TRUST_STORE_ALGORITHM_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreMgrFactoryAlgorithm);
        return trustStoreMgrFactoryAlgorithm;
    }

    public static SSLContext getSSLContext(String protocol,
                                           KeyManager[] keyStoreManagers,
                                           TrustManager[] trustStoreManagers)
        throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance(protocol);
        ctx.init(keyStoreManagers, trustStoreManagers, null);
        return ctx;
    }

    public static String[] getSupportedCipherSuites(SSLContext context) {
        return context.getSocketFactory().getSupportedCipherSuites();
    }

    public static String[] getServerSupportedCipherSuites(SSLContext context) {
        return context.getServerSocketFactory().getSupportedCipherSuites();
    }

    public static String[] getCiphersuitesToInclude(List<String> cipherSuitesList,
                                           FiltersType filters,
                                           String[] defaultCipherSuites,
                                           String[] supportedCipherSuites,
                                           Logger log) {
        // CipherSuites are returned in the following priority:
        // 1) If we have defined explicit "cipherSuite" configuration
        // 2) If we have defined ciphersuites via a system property.
        // 3) The default JVM CipherSuites, if no filters have been defined
        // 4) Filter the supported cipher suites (*not* the default JVM CipherSuites)
        if (!(cipherSuitesList == null || cipherSuitesList.isEmpty())) {
            return getCiphersFromList(cipherSuitesList, log, false);
        }

        String[] cipherSuites = getSystemCiphersuites(log);
        if (cipherSuites != null) {
            return cipherSuites;
        }

        // If we have no explicit cipherSuites (for the include case as above), and no filters,
        // then just use the defaults
        if ((defaultCipherSuites != null && defaultCipherSuites.length != 0)
            && (filters == null || !(filters.isSetInclude() || filters.isSetExclude()))) {
            LogUtils.log(log, Level.FINE, "CIPHERSUITES_SET", defaultCipherSuites.toString());
            return defaultCipherSuites;
        }

        LogUtils.log(log, Level.FINE, "CIPHERSUITES_NOT_SET");

        return getFilteredCiphersuites(filters, supportedCipherSuites, log, false);
    }

    public static String[] getFilteredCiphersuites(FiltersType filters,
                                           String[] supportedCipherSuites,
                                           Logger log, boolean exclude) {
        // We have explicit filters, so use the "include/exclude" cipherSuiteFilter configuration
        List<String> filteredCipherSuites = new ArrayList<>();
        List<String> excludedCipherSuites = new ArrayList<>();
        List<Pattern> includes =
            filters != null
                ? compileRegexPatterns(filters.getInclude(), true, log)
                : compileRegexPatterns(DEFAULT_CIPHERSUITE_FILTERS_INCLUDE, true, log);
        List<Pattern> excludes =
            filters != null
                ? compileRegexPatterns(filters.getExclude(), false, log)
                : compileRegexPatterns(DEFAULT_CIPHERSUITE_FILTERS_EXCLUDE, true, log);
        for (int i = 0; i < supportedCipherSuites.length; i++) {
            if (matchesOneOf(supportedCipherSuites[i], includes)
                && !matchesOneOf(supportedCipherSuites[i], excludes)) {
                LogUtils.log(log,
                             Level.FINE,
                             "CIPHERSUITE_INCLUDED",
                             supportedCipherSuites[i]);
                filteredCipherSuites.add(supportedCipherSuites[i]);
            } else {
                LogUtils.log(log,
                             Level.FINE,
                             "CIPHERSUITE_EXCLUDED",
                             supportedCipherSuites[i]);
                excludedCipherSuites.add(supportedCipherSuites[i]);
            }
        }
        LogUtils.log(log,
                     Level.FINE,
                     "CIPHERSUITES_FILTERED",
                     filteredCipherSuites);
        LogUtils.log(log,
                     Level.FINE,
                     "CIPHERSUITES_EXCLUDED",
                     excludedCipherSuites);
        if (exclude) {
            return getCiphersFromList(excludedCipherSuites, log, exclude);
        } else {
            return getCiphersFromList(filteredCipherSuites, log, exclude);
        }
    }

    private static String[] getSystemCiphersuites(Logger log) {
        String jvmCipherSuites = System.getProperty(HTTPS_CIPHER_SUITES);
        if ((jvmCipherSuites != null) && (!jvmCipherSuites.isEmpty())) {
            LogUtils.log(log, Level.FINE, "CIPHERSUITES_SYSTEM_PROPERTY_SET", jvmCipherSuites);
            return jvmCipherSuites.split(",");
        } else {
            return null;
        }

    }

    private static List<Pattern> compileRegexPatterns(List<String> regexes,
                                                      boolean include,
                                                      Logger log) {
        List<Pattern> patterns = new ArrayList<>();
        if (regexes != null) {
            String msg = include
                         ? "CIPHERSUITE_INCLUDE_FILTER"
                         : "CIPHERSUITE_EXCLUDE_FILTER";
            for (String s : regexes) {
                LogUtils.log(log, Level.FINE, msg, s);
                patterns.add(Pattern.compile(s));
            }
        }
        return patterns;
    }

    private static boolean matchesOneOf(String s, List<Pattern> patterns) {
        boolean matches = false;
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.matches()) {
                    matches = true;
                    break;
                }
            }
        }
        return matches;
    }

    private static String[] getCiphersFromList(List<String> cipherSuitesList,
                                               Logger log,
                                               boolean exclude) {
        int numCipherSuites = cipherSuitesList.size();
        String[] cipherSuites = cipherSuitesList.toArray(new String[numCipherSuites]);
        if (log.isLoggable(Level.FINE)) {
            StringBuilder ciphsStr = new StringBuilder();
            for (String s : cipherSuites) {
                if (ciphsStr.length() != 0) {
                    ciphsStr.append(", ");
                }
                ciphsStr.append(s);
            }
            LogUtils.log(log, Level.FINE,
                exclude ? "CIPHERSUITES_EXCLUDED" : "CIPHERSUITES_SET", ciphsStr.toString());
        }
        return cipherSuites;
    }

    public static String getTrustStoreType(String trustStoreType, Logger log) {
        return getTrustStoreType(trustStoreType, log, DEFAULT_TRUST_STORE_TYPE);
    }

    public static String getTrustStoreType(String trustStoreType, Logger log, String def) {
        String logMsg = null;
        if (trustStoreType != null) {
            logMsg = "TRUST_STORE_TYPE_SET";
        } else {
            //Can default to JKS
            trustStoreType = SystemPropertyAction.getProperty("javax.net.ssl.trustStoreType");
            if (trustStoreType == null) {
                trustStoreType = def;
                logMsg = "TRUST_STORE_TYPE_NOT_SET";
            } else {
                logMsg = "TRUST_STORE_TYPE_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreType);
        return trustStoreType;
    }

    public static String getTruststorePassword(String trustStorePassword,
                                             Logger log) {
        String logMsg = null;
        if (trustStorePassword != null) {
            logMsg = "TRUST_STORE_PASSWORD_SET";
        } else {
            trustStorePassword =
                SystemPropertyAction.getProperty("javax.net.ssl.trustStorePassword");
            logMsg = trustStorePassword != null
                     ? "TRUST_STORE_PASSWORD_SYSTEM_PROPERTY_SET"
                     : "TRUST_STORE_PASSWORD_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg);
        return trustStorePassword;
    }

    public static String getTruststoreProvider(String trustStoreProvider, Logger log) {
        String logMsg = null;
        if (trustStoreProvider != null) {
            logMsg = "TRUST_STORE_PROVIDER_SET";
        } else {
            trustStoreProvider = SystemPropertyAction.getProperty("javax.net.ssl.trustStoreProvider", null);
            if (trustStoreProvider == null) {
                logMsg = "TRUST_STORE_PROVIDER_NOT_SET";
            } else {
                logMsg = "TRUST_STORE_PROVIDER_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreProvider);
        return trustStoreProvider;
    }

    public static String getSecureSocketProtocol(String secureSocketProtocol,
                                                 Logger log) {
        if (secureSocketProtocol != null) {
            LogUtils.log(log,
                         Level.FINE,
                         "SECURE_SOCKET_PROTOCOL_SET",
                         secureSocketProtocol);
        } else {
            LogUtils.log(log, Level.FINE, "SECURE_SOCKET_PROTOCOL_NOT_SET");
            secureSocketProtocol = DEFAULT_SECURE_SOCKET_PROTOCOL;
        }
        return secureSocketProtocol;
    }

}
