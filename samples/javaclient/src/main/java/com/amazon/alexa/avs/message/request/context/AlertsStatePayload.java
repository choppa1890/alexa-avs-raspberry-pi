/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.message.request.context;

import com.amazon.alexa.avs.Alert;
import com.amazon.alexa.avs.message.Payload;

import java.util.List;

public final class AlertsStatePayload extends Payload {

    private final List<Alert> allAlerts;
    private final List<Alert> activeAlerts;

    public AlertsStatePayload(List<Alert> all, List<Alert> active) {
        this.allAlerts = all;
        this.activeAlerts = active;
    }

    public List<Alert> getAllAlerts() {
        return allAlerts;
    }

    public List<Alert> getActiveAlerts() {
        return activeAlerts;
    }
}
