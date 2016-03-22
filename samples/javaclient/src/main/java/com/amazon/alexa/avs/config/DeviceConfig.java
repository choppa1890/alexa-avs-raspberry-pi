/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.config;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Container that encapsulates all the information that exists in the config file.
 */
public class DeviceConfig {
    private static final String DEFAULT_HOST = "https://avs-alexa-na.amazon.com";
    public static final String FILE_NAME = "config.json";

    public static final String PRODUCT_ID = "productId";
    public static final String DSN = "dsn";
    public static final String COMPANION_APP = "companionApp";
    public static final String COMPANION_SERVICE = "companionService";
    public static final String PROVISIONING_METHOD = "provisioningMethod";
    public static final String AVS_HOST = "avsHost";

    /*
     * Required parameters from the config file.
     */
    private final String productId;
    private final String dsn;
    private final ProvisioningMethod provisioningMethod;
    private final URL avsHost;

    /*
     * Optional parameters from the config file.
     */
    private CompanionAppInformation companionAppInfo;
    private CompanionServiceInformation companionServiceInfo;

    @SuppressWarnings("javadoc")
    public enum ProvisioningMethod {
        COMPANION_APP(DeviceConfig.COMPANION_APP), COMPANION_SERVICE(
                DeviceConfig.COMPANION_SERVICE);

        private String name;

        ProvisioningMethod(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static ProvisioningMethod fromString(String method) {
            if (ProvisioningMethod.COMPANION_APP.toString().equals(method)) {
                return COMPANION_APP;
            } else if (ProvisioningMethod.COMPANION_SERVICE.toString().equals(method)) {
                return COMPANION_SERVICE;
            }
            throw new IllegalArgumentException("Invalid provisioning method");
        }
    }

    /**
     * Creates a {@link DeviceConfig} object.
     *
     * @param productId
     *            The productId of this device.
     * @param dsn
     *            The dsn of this device.
     * @param provisioningMethod
     *            The provisioningMethod to use. One of: {@value #COMPANION_APP},
     *            {@value #COMPANION_SERVICE}
     * @param companionAppInfo
     *            The information necessary for the Companion App method of provisioning.
     * @param companionServiceInfo
     *            The information necessary for the Companion Service method of provisioning.
     * @param avsHost
     *            (optional) AVS host override
     */
    public DeviceConfig(String productId, String dsn, String provisioningMethod,
            CompanionAppInformation companionAppInfo,
            CompanionServiceInformation companionServiceInfo, String avsHost) {

        if (StringUtils.isBlank(productId)) {
            throw new MalformedConfigException(PRODUCT_ID + " is blank in your config file.");
        }

        if (StringUtils.isBlank(dsn)) {
            throw new MalformedConfigException(DSN + " is blank in your config file.");
        }

        ProvisioningMethod method;
        try {
            method = ProvisioningMethod.fromString(provisioningMethod);
        } catch (IllegalArgumentException e) {
            throw new MalformedConfigException(PROVISIONING_METHOD + " should be either \""
                    + COMPANION_APP + "\" or \"" + COMPANION_SERVICE + "\".");
        }

        if (method == ProvisioningMethod.COMPANION_APP
                && (companionAppInfo == null || !companionAppInfo.isValid())) {
            throw new MalformedConfigException("Your " + PROVISIONING_METHOD + " is set to \""
                    + COMPANION_APP + "\" but you do not have a valid \"" + COMPANION_APP
                    + "\" section in your config file.");
        } else if (method == ProvisioningMethod.COMPANION_SERVICE
                && (companionServiceInfo == null || !companionServiceInfo.isValid())) {
            throw new MalformedConfigException("Your " + PROVISIONING_METHOD + " is set to \""
                    + COMPANION_SERVICE + "\" but you do not have a valid \"" + COMPANION_SERVICE
                    + "\" section in your config file.");
        }

        this.provisioningMethod = method;
        this.productId = productId;
        this.dsn = dsn;
        this.companionServiceInfo = companionServiceInfo;
        this.companionAppInfo = companionAppInfo;
        avsHost = StringUtils.isBlank(avsHost) ? DEFAULT_HOST : avsHost;
        try {
            this.avsHost = new URL(avsHost);
        } catch (MalformedURLException e) {
            throw new MalformedConfigException(AVS_HOST + " is malformed in your config file.", e);
        }
    }

    public DeviceConfig(String productId, String dsn, String provisioningMethod,
            CompanionAppInformation companionAppInfo,
            CompanionServiceInformation companionServiceInfo) {
        this(productId, dsn, provisioningMethod, companionAppInfo, companionServiceInfo,
                DEFAULT_HOST);
    }

    /**
     * @return avsHost.
     */
    public URL getAvsHost() {
        return avsHost;
    }

    /**
     * @return productId.
     */
    public String getProductId() {
        return productId;
    }

    /**
     * @return dsn.
     */
    public String getDsn() {
        return dsn;
    }

    /**
     * @return provisioningMethod.
     */
    public ProvisioningMethod getProvisioningMethod() {
        return provisioningMethod;
    }

    /**
     * @return companionAppInfo.
     */
    public CompanionAppInformation getCompanionAppInfo() {
        return companionAppInfo;
    }

    /**
     * @param companionAppInfo
     */
    public void setCompanionAppInfo(CompanionAppInformation companionAppInfo) {
        this.companionAppInfo = companionAppInfo;
    }

    /**
     * @return companionServiceInfo.
     */
    public CompanionServiceInformation getCompanionServiceInfo() {
        return companionServiceInfo;
    }

    /**
     * @param companionServiceInfo
     */
    public void setCompanionServiceInfo(CompanionServiceInformation companionServiceInfo) {
        this.companionServiceInfo = companionServiceInfo;
    }

    /**
     * Save this file back to disk.
     */
    public void saveConfig() {
        DeviceConfigUtils.updateConfigFile(this);
    }

    /**
     * Serialize this object to JSON.
     *
     * @return A JSON representation of this object.
     */
    public JsonObject toJson() {
        JsonObjectBuilder builder = Json
                .createObjectBuilder()
                .add(PRODUCT_ID, productId)
                .add(DSN, dsn)
                .add(PROVISIONING_METHOD, provisioningMethod.toString())
                .add(AVS_HOST, avsHost.toString());

        if (companionAppInfo != null) {
            builder.add(COMPANION_APP, companionAppInfo.toJson());
        }

        if (companionServiceInfo != null) {
            builder.add(COMPANION_SERVICE, companionServiceInfo.toJson());
        }

        return builder.build();
    }

    /**
     * Describes the information necessary for the Companion App method of provisioning.
     */
    public static class CompanionAppInformation {
        public static final String LOCAL_PORT = "localPort";
        public static final String LWA_URL = "lwaUrl";
        public static final String SSL_KEYSTORE = "sslKeyStore";
        public static final String SSL_KEYSTORE_PASSPHRASE = "sslKeyStorePassphrase";
        public static final String REFRESH_TOKEN = "refreshToken";
        public static final String CLIENT_ID = "clientId";

        private final int localPort;
        private final String lwaUrl;
        private final String sslKeyStore;
        private final String sslKeyStorePassphrase;

        private URL loginWithAmazonUrl;
        private String clientId;
        private String refreshToken;

        /**
         * Creates a {@link CompanionAppInformation} object.
         *
         * @param localPort
         * @param lwaUrl
         */
        public CompanionAppInformation(int localPort, String lwaUrl, String sslKeyStore,
                String sslKeyStorePassphrase) {
            this.localPort = localPort;
            this.sslKeyStore = sslKeyStore;
            this.sslKeyStorePassphrase = sslKeyStorePassphrase;
            this.lwaUrl = lwaUrl;
        }

        /**
         * @return clientId.
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * @param clientId
         */
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        /**
         * @return refreshToken.
         */
        public String getRefreshToken() {
            return refreshToken;
        }

        /**
         * @param refreshToken
         */
        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        /**
         * @return localPort.
         */
        public int getLocalPort() {
            return localPort;
        }

        /**
         * @return lwaUrl.
         */
        public URL getLwaUrl() {
            if (loginWithAmazonUrl == null) {
                if (StringUtils.isBlank(lwaUrl)) {
                    throw new MalformedConfigException(LWA_URL + " is blank in your config file.");
                } else {
                    try {
                        loginWithAmazonUrl = new URL(lwaUrl);
                    } catch (MalformedURLException e) {
                        throw new MalformedConfigException(
                                LWA_URL + " is malformed in your config file.", e);
                    }
                }
            }
            return loginWithAmazonUrl;
        }

        /**
         * @return sslKeyStore.
         */
        public String getSslKeyStore() {
            return sslKeyStore;
        }

        /**
         * @return sslKeyStorePassphrase.
         */
        public String getSslKeyStorePassphrase() {
            return sslKeyStorePassphrase;
        }

        /**
         * Serialize this object to JSON.
         *
         * @return A JSON representation of this object.
         */
        public JsonObject toJson() {
            JsonObjectBuilder builder = Json
                    .createObjectBuilder()
                    .add(LOCAL_PORT, localPort)
                    .add(LWA_URL, getLwaUrl().toString())
                    .add(SSL_KEYSTORE, sslKeyStore)
                    .add(SSL_KEYSTORE_PASSPHRASE, sslKeyStorePassphrase);

            if ((clientId != null) && (refreshToken != null)) {
                builder.add(CLIENT_ID, clientId);
                builder.add(REFRESH_TOKEN, refreshToken);
            }

            return builder.build();
        }

        public boolean isValid() {
            if (localPort < 1 || localPort > 65535) {
                throw new MalformedConfigException(
                        LOCAL_PORT + " is invalid. Value port values are 1-65535.");
            }

            getLwaUrl(); // Verifies that the url is valid
            if (StringUtils.isBlank(sslKeyStore)) {
                throw new MalformedConfigException(SSL_KEYSTORE + " is blank in your config file.");
            } else {
                File sslKeyStoreFile = new File(sslKeyStore);
                if (!sslKeyStoreFile.exists()) {
                    throw new MalformedConfigException(
                            sslKeyStore + " " + SSL_KEYSTORE + " does not exist.");
                }
            }
            return true;
        }
    }

    /**
     * Describes the information necessary for the Companion Service method of provisioning.
     */
    public static class CompanionServiceInformation {
        public static final String SESSION_ID = "sessionId";
        public static final String SERVICE_URL = "serviceUrl";
        public static final String SSL_CLIENT_KEYSTORE = "sslClientKeyStore";
        public static final String SSL_CLIENT_KEYSTORE_PASSPHRASE = "sslClientKeyStorePassphrase";
        public static final String SSL_CA_CERT = "sslCaCert";

        private final String serviceUrlString;
        private final String sslClientKeyStore;
        private final String sslClientKeyStorePassphrase;
        private final String sslCaCert;

        private URL serviceUrl;
        private String sessionId;

        /**
         * Creates a {@link CompanionServiceInformation} object.
         *
         * @param serviceUrl
         */
        public CompanionServiceInformation(String serviceUrl, String sslClientKeyStore,
                String sslClientKeyStorePassphrase, String sslCaCert) {
            this.serviceUrlString = serviceUrl;
            this.sslClientKeyStore = sslClientKeyStore;
            this.sslClientKeyStorePassphrase = sslClientKeyStorePassphrase;
            this.sslCaCert = sslCaCert;
        }

        /**
         * @return serviceUrl.
         */
        public URL getServiceUrl() {
            if (serviceUrl == null) {
                if (StringUtils.isBlank(serviceUrlString)) {
                    throw new MalformedConfigException(
                            SERVICE_URL + " is blank in your config file.");
                } else {
                    try {
                        this.serviceUrl = new URL(serviceUrlString);
                    } catch (MalformedURLException e) {
                        throw new MalformedConfigException(
                                SERVICE_URL + " is malformed in your config file.", e);
                    }
                }
            }
            return serviceUrl;
        }

        /**
         * @param sessionId
         */
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * @return sessionId.
         */
        public String getSessionId() {
            return sessionId;
        }

        /**
         * @return sslClientKeyStore.
         */
        public String getSslClientKeyStore() {
            return sslClientKeyStore;
        }

        /**
         * @return sslClientKeyStorePassphrase.
         */
        public String getSslClientKeyStorePassphrase() {
            return sslClientKeyStorePassphrase;
        }

        /**
         * @return sslCaCert.
         */
        public String getSslCaCert() {
            return sslCaCert;
        }

        /**
         * Serialize this object to JSON.
         *
         * @return A JSON representation of this object.
         */
        public JsonObject toJson() {
            JsonObjectBuilder builder = Json
                    .createObjectBuilder()
                    .add(SERVICE_URL, getServiceUrl().toString())
                    .add(SSL_CLIENT_KEYSTORE, sslClientKeyStore)
                    .add(SSL_CLIENT_KEYSTORE_PASSPHRASE, sslClientKeyStorePassphrase)
                    .add(SSL_CA_CERT, sslCaCert);

            if (sessionId != null) {
                builder.add(SESSION_ID, sessionId);
            }

            return builder.build();
        }

        public boolean isValid() {
            getServiceUrl(); // Verifies that the URL is valid
            if (StringUtils.isBlank(sslClientKeyStore)) {
                throw new MalformedConfigException(
                        SSL_CLIENT_KEYSTORE + " is blank in your config file.");
            } else {
                File sslClientKeyStoreFile = new File(sslClientKeyStore);
                if (!sslClientKeyStoreFile.exists()) {
                    throw new MalformedConfigException(
                            sslClientKeyStore + " " + SSL_CLIENT_KEYSTORE + " does not exist.");
                }
            }

            if (StringUtils.isBlank(sslCaCert)) {
                throw new MalformedConfigException(SSL_CA_CERT + " is blank in your config file.");
            } else {
                File sslCaCertFile = new File(sslCaCert);
                if (!sslCaCertFile.exists()) {
                    throw new MalformedConfigException(
                            sslCaCertFile + " " + SSL_CA_CERT + " does not exist.");
                }
            }
            return true;
        }
    }

    @SuppressWarnings("javadoc")
    public static class MalformedConfigException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MalformedConfigException(String message, Throwable cause) {
            super(message, cause);
        }

        public MalformedConfigException(String s) {
            super(s);
        }
    }
}
