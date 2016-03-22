/**
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.message.response.ProgressReport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AudioPlayerProgressReporter {
    private final ScheduledExecutorService eventScheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> progressReportDelayFuture;
    private ScheduledFuture<?> progressReportIntervalFuture;

    private final Runnable progressReportDelayRunnable;
    private final Runnable progressReportIntervalRunnable;

    private long progressReportDelay;
    private long progressReportInterval;

    private long activeTimestampMs;
    private long totalActiveTimeElapsedMs;

    public AudioPlayerProgressReporter(Runnable progressReportDelayRunnable,
            Runnable progressReportIntervalRunnable) {
        this.progressReportDelayRunnable = progressReportDelayRunnable;
        this.progressReportIntervalRunnable = progressReportIntervalRunnable;
    }

    public synchronized void start(ProgressReport progressReport) {
        if (progressReport == null) {
            throw new IllegalArgumentException("ProgressReport must not be null.");
        }

        progressReportDelay = progressReport.getProgressReportDelayInMilliseconds();
        progressReportInterval = progressReport.getProgressReportIntervalInMilliseconds();

        scheduleBothEvents(progressReportDelay, 0, progressReportInterval);
    }

    public synchronized void resume() {
        long remainingDelay = Math.max(0, progressReportDelay - totalActiveTimeElapsedMs);
        long remainingIntervalDelay = progressReportInterval == 0 ? 0
                : progressReportInterval - totalActiveTimeElapsedMs % progressReportInterval;
        scheduleBothEvents(remainingDelay, remainingIntervalDelay, progressReportInterval);
    }

    /**
     * Schedules both events.
     *
     * @param delay
     *            Delay in ms for the progress report delay event.
     * @param intervalDelay
     *            Delay in ms for the progress report interval event.
     * @param interval
     *            Period in ms of the progress report interval event.
     */
    private void scheduleBothEvents(long delay, long intervalDelay, long interval) {
        if (delay != 0) {
            progressReportDelayFuture = eventScheduler.schedule(progressReportDelayRunnable, delay,
                    TimeUnit.MILLISECONDS);
        }

        if (interval != 0) {
            progressReportIntervalFuture = eventScheduler.scheduleAtFixedRate(
                    progressReportIntervalRunnable, intervalDelay, interval, TimeUnit.MILLISECONDS);
        }

        if (isStarted()) {
            activeTimestampMs = System.currentTimeMillis();
        }
    }

    private boolean isStarted() {
        return progressReportDelayFuture != null || progressReportIntervalFuture != null;
    }

    public synchronized void stop() {
        cancelEvents();
        totalActiveTimeElapsedMs = 0;
    }

    public synchronized void pause() {
        cancelEvents();
        totalActiveTimeElapsedMs += System.currentTimeMillis() - activeTimestampMs;
    }

    private void cancelEvents() {
        if (progressReportDelayFuture != null && !progressReportDelayFuture.isDone()) {
            progressReportDelayFuture.cancel(false);
        }

        if (progressReportIntervalFuture != null && !progressReportIntervalFuture.isDone()) {
            progressReportIntervalFuture.cancel(false);
        }
    }
}
