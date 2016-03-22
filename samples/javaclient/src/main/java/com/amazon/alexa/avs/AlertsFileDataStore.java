/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.AlertManager.ResultListener;
import com.amazon.alexa.avs.config.ObjectMapperFactory;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.JsonReader;

/**
 * A file-backed data store for AVS Alerts
 */
public class AlertsFileDataStore implements AlertsDataStore {
    private static final Logger log = LoggerFactory.getLogger(AlertsFileDataStore.class);
    private static final String ALARM_FILE = "alarms.json";
    private static final int MINUTES_AFTER_PAST_ALERT_EXPIRES = 30;
    private static AlertsFileDataStore sInstance = new AlertsFileDataStore();
    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();

    private AlertsFileDataStore() {
    }

    public synchronized static AlertsFileDataStore getInstance() {
        return sInstance;
    }

    @Override
    public synchronized void loadFromDisk(AlertManager manager, final ResultListener listener) {
        sExecutor.execute(new Runnable() {
            @Override
            public void run() {
                FileReader fis = null;
                BufferedReader br = null;
                JsonReader parser = null;

                ObjectReader reader = ObjectMapperFactory
                        .getObjectReader()
                        .withType(new TypeReference<List<Alert>>() {
                });
                List<Alert> droppedAlerts = new LinkedList<Alert>();
                try {
                    fis = new FileReader(ALARM_FILE);
                    br = new BufferedReader(fis);

                    List<Alert> alerts = reader.readValue(br);
                    for (Alert alert : alerts) {
                        // Only add alerts that are within the expiration window
                        if (alert.getScheduledTime().isAfter(ZonedDateTime
                                .now()
                                .minusMinutes(MINUTES_AFTER_PAST_ALERT_EXPIRES))) {
                            manager.add(alert, true);
                        } else {
                            droppedAlerts.add(alert);
                        }
                    }
                    // Now that all the valid alerts have been re-added to the alarm manager,
                    // go through and explicitly drop all the alerts that were not added
                    for (Alert alert : droppedAlerts) {
                        manager.drop(alert);
                    }
                    listener.onSuccess();
                } catch (FileNotFoundException e) {
                    // This is not a fatal error
                    // The alarm file might not have been created yet
                    listener.onSuccess();
                } catch (IOException e) {
                    log.error("Failed to load alerts from disk.", e);
                    listener.onFailure();
                } finally {
                    IOUtils.closeQuietly(parser);
                    IOUtils.closeQuietly(br);
                }
            }
        });
    }

    @Override
    public synchronized void writeToDisk(List<Alert> alerts, final ResultListener listener) {
        sExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ObjectWriter writer = ObjectMapperFactory.getObjectWriter();
                PrintWriter out = null;
                try {
                    out = new PrintWriter(ALARM_FILE);
                    out.print(writer.writeValueAsString(alerts));
                    out.flush();
                    listener.onSuccess();
                } catch (IOException e) {
                    log.error("Failed to write to disk", e);
                    listener.onFailure();
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        });
    }
}
