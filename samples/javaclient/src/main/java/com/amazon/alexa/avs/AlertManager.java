/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.message.request.context.AlertsStatePayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlertManager implements AlertHandler {
    private final AlertEventListener listener;
    private final AlertHandler handler;
    private final Map<String, AlertScheduler> schedulers;
    private final Set<String> activeAlerts;
    private final AlertsDataStore dataStore;

    private static final Logger log = LoggerFactory.getLogger(AlertManager.class);

    public AlertManager(AlertEventListener listener, AlertHandler handler,
            AlertsDataStore dataStore) {
        this.listener = listener;
        this.handler = handler;
        this.schedulers = new HashMap<String, AlertScheduler>();
        this.activeAlerts = new HashSet<String>();
        this.dataStore = dataStore;
    }

    void loadFromDisk(final ResultListener listener) {
        dataStore.loadFromDisk(AlertManager.this, listener);

    }

    public synchronized boolean hasAlert(String alertToken) {
        return schedulers.containsKey(alertToken);
    }

    public synchronized boolean hasActiveAlerts() {
        return activeAlerts.size() > 0;
    }

    public synchronized Set<String> getActiveAlerts() {
        return activeAlerts;
    }

    public synchronized List<Alert> getAllAlerts() {
        List<Alert> list = new ArrayList<Alert>(schedulers.size());
        for (AlertScheduler scheduler : schedulers.values()) {
            list.add(scheduler.getAlert());
        }
        return list;
    }

    public synchronized AlertScheduler getScheduler(String alertToken) {
        return schedulers.get(alertToken);
    }

    public void add(final Alert alert) {
        add(alert, false);
    }

    // When re-adding alerts by reading them from disk, suppressEvent
    // should be set to true. We only want to trigger events the first time
    // a alert is set
    public synchronized void add(final Alert alert, final boolean suppressEvent) {
        final AlertScheduler scheduler = new AlertScheduler(alert, this);
        schedulers.put(alert.getToken(), scheduler);
        log.debug("Adding alert with token {}", alert.getToken());
        writeCurrentAlertsToDisk(new ResultListener() {
            @Override
            public void onSuccess() {
                if (!suppressEvent) {
                    listener.onAlertSet(alert.getToken(), true);
                }
            }

            @Override
            public void onFailure() {
                if (!suppressEvent) {
                    listener.onAlertSet(alert.getToken(), false);
                }
                schedulers.remove(alert.getToken());
                scheduler.cancel();
            }
        });
    }

    public synchronized void delete(final String alertToken) {
        final AlertScheduler scheduler = schedulers.remove(alertToken);
        log.debug("Deleting alert with token {}", alertToken);
        if (scheduler != null) {
            final Alert alert = scheduler.getAlert();
            writeCurrentAlertsToDisk(new ResultListener() {
                @Override
                public void onSuccess() {
                    scheduler.cancel();
                    listener.onAlertDelete(alert.getToken(), true);
                }

                @Override
                public void onFailure() {
                    listener.onAlertDelete(alert.getToken(), false);
                }
            });
        }
    }

    public void drop(final Alert alert) {
        listener.onAlertStopped(alert.getToken());
    }

    private void writeCurrentAlertsToDisk(final ResultListener l) {
        dataStore.writeToDisk(getAllAlerts(), l);
    }

    @Override
    public synchronized void startAlert(String alertToken) {
        activeAlerts.add(alertToken);
        listener.onAlertStarted(alertToken);
        handler.startAlert(alertToken);
    }

    @Override
    public synchronized void stopAlert(String alertToken) {
        activeAlerts.remove(alertToken);
        schedulers.remove(alertToken);
        listener.onAlertStopped(alertToken);
        handler.stopAlert(alertToken);
    }

    /**
     * Stops an active alert
     */
    public synchronized void stopActiveAlert() {
        if (hasActiveAlerts()) {
            for (String alertToken : activeAlerts) {
                stopAlert(alertToken);
                return;
            }
        }
    }

    public synchronized AlertsStatePayload getState() {
        List<Alert> all = new ArrayList<>(schedulers.size());
        List<Alert> active = new ArrayList<>(activeAlerts.size());
        for (AlertScheduler scheduler : schedulers.values()) {
            Alert alert = scheduler.getAlert();
            all.add(alert);

            if (activeAlerts.contains(alert.getToken())) {
                active.add(alert);
            }
        }
        return new AlertsStatePayload(all, active);
    }

    interface ResultListener {
        void onSuccess();

        void onFailure();
    }
}
