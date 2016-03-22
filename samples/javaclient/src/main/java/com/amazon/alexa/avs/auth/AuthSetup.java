/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.auth;

import com.amazon.alexa.avs.auth.companionapp.CodeChallengeWorkflow;
import com.amazon.alexa.avs.auth.companionapp.CompanionAppAuthManager;
import com.amazon.alexa.avs.auth.companionapp.OAuth2ClientForPkce;
import com.amazon.alexa.avs.auth.companionapp.server.CompanionAppProvisioningServer;
import com.amazon.alexa.avs.auth.companionservice.CompanionServiceAuthManager;
import com.amazon.alexa.avs.auth.companionservice.CompanionServiceClient;
import com.amazon.alexa.avs.auth.companionservice.RegCodeDisplayHandler;
import com.amazon.alexa.avs.config.DeviceConfig;
import com.amazon.alexa.avs.config.DeviceConfig.ProvisioningMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Initializes and owns the two ways to provision this device: via a companion service where this
 * device acts as a client, and via a companion application where this device acts as a server.
 */
public class AuthSetup implements AccessTokenListener {

    private static final Logger log = LoggerFactory.getLogger(AuthSetup.class);

    private final DeviceConfig deviceConfig;
    private final RegCodeDisplayHandler regCodeDisplayHandler;
    private final Set<AccessTokenListener> accessTokenListeners = new HashSet<>();

    /**
     * Creates an {@link AuthSetup} object.
     *
     * @param deviceConfig
     *            Information about this device.
     * @param regCodeDisplayHandler
     */
    public AuthSetup(final DeviceConfig deviceConfig, final RegCodeDisplayHandler regCodeDisplayHandler) {
        this.deviceConfig = deviceConfig;
        this.regCodeDisplayHandler = regCodeDisplayHandler;
    }

    public void addAccessTokenListener(AccessTokenListener accessTokenListener) {
        accessTokenListeners.add(accessTokenListener);
    }

    /**
     * Initializes threads for the {@link CompanionAppProvisioningServer} and the
     * {@link CompanionServiceClient}, depending on which is selected by the user.
     */
    public void startProvisioningThread() {
        if (deviceConfig.getProvisioningMethod() == ProvisioningMethod.COMPANION_APP) {
            OAuth2ClientForPkce oAuthClient =
                    new OAuth2ClientForPkce(deviceConfig.getCompanionAppInfo().getLwaUrl());
            CompanionAppAuthManager authManager = new CompanionAppAuthManager(deviceConfig,
                    oAuthClient, CodeChallengeWorkflow.getInstance(), this);

            final CompanionAppProvisioningServer registrationServer =
                    new CompanionAppProvisioningServer(authManager, deviceConfig);

            Thread provisioningThread = new Thread() {
                @Override
                public void run() {
                    try {
                        registrationServer.startServer();
                    } catch (Exception e) {
                        log.error("Failed to start companion app provisioning server", e);
                    }
                }
            };
            provisioningThread.start();
        } else if (deviceConfig.getProvisioningMethod() == ProvisioningMethod.COMPANION_SERVICE) {
            CompanionServiceClient remoteProvisioningClient =
                    new CompanionServiceClient(deviceConfig);
            final CompanionServiceAuthManager authManager = new CompanionServiceAuthManager(
                    deviceConfig, remoteProvisioningClient, regCodeDisplayHandler, this);

            Thread provisioningThread = new Thread() {
                @Override
                public void run() {
                    try {
                        authManager.startRemoteProvisioning();
                    } catch (Exception e) {
                        log.error("Failed to start companion service client", e);
                    }
                }
            };
            provisioningThread.start();
        }
    }

    @Override
    public void onAccessTokenReceived(String accessToken) {
        accessTokenListeners.stream().forEach(listener -> listener.onAccessTokenReceived(accessToken));
    }
}
