/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.AVSAudioPlayer.AlexaSpeechListener;
import com.amazon.alexa.avs.AlertManager.ResultListener;
import com.amazon.alexa.avs.auth.AccessTokenListener;
import com.amazon.alexa.avs.exception.DirectiveHandlingException;
import com.amazon.alexa.avs.exception.DirectiveHandlingException.ExceptionType;
import com.amazon.alexa.avs.http.AVSClient;
import com.amazon.alexa.avs.http.AVSClientFactory;
import com.amazon.alexa.avs.http.ParsingFailedHandler;
import com.amazon.alexa.avs.message.request.RequestBody;
import com.amazon.alexa.avs.message.request.RequestFactory;
import com.amazon.alexa.avs.message.response.Directive;
import com.amazon.alexa.avs.message.response.alerts.DeleteAlert;
import com.amazon.alexa.avs.message.response.alerts.SetAlert;
import com.amazon.alexa.avs.message.response.alerts.SetAlert.AlertType;
import com.amazon.alexa.avs.message.response.audioplayer.ClearQueue;
import com.amazon.alexa.avs.message.response.audioplayer.Play;
import com.amazon.alexa.avs.message.response.speaker.SetMute;
import com.amazon.alexa.avs.message.response.speaker.VolumePayload;
import com.amazon.alexa.avs.message.response.speechsynthesizer.Speak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AVSController
        implements RecordingStateListener, AlertHandler, AlertEventListener, AccessTokenListener,
        DirectiveDispatcher, AlexaSpeechListener, ParsingFailedHandler, UserActivityListener {
    private final AudioCapture microphone;
    private final AVSClient avsClient;
    private final DialogRequestIdAuthority dialogRequestIdAuthority;
    private AlertManager alertManager;

    private boolean eventRunning = false; // is an event currently being sent

    private static final AudioInputFormat AUDIO_TYPE = AudioInputFormat.LPCM;
    private static final String START_SOUND = "res/start.mp3";
    private static final String END_SOUND = "res/stop.mp3";
    private static final String ERROR_SOUND = "res/error.mp3";
    private static final SpeechProfile PROFILE = SpeechProfile.CLOSE_TALK;
    private static final String FORMAT = "AUDIO_L16_RATE_16000_CHANNELS_1";

    private static final Logger log = LoggerFactory.getLogger(AVSController.class);
    private static final long MILLISECONDS_PER_SECOND = 1000;
    private static final long USER_INACTIVITY_REPORT_PERIOD_HOURS = 1;

    private final AVSAudioPlayer player;
    private BlockableDirectiveThread dependentDirectiveThread;
    private BlockableDirectiveThread independentDirectiveThread;
    private BlockingQueue<Directive> dependentQueue;
    private BlockingQueue<Directive> independentQueue;
    public SpeechRequestAudioPlayerPauseController speechRequestAudioPlayerPauseController;

    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private AtomicLong lastUserInteractionTimestampSeconds;

    private final Set<ExpectSpeechListener> expectSpeechListeners;

    public AVSController(ExpectSpeechListener listenHandler, AVSAudioPlayerFactory audioFactory,
            AlertManagerFactory alarmFactory, AVSClientFactory avsClientFactory,
            DialogRequestIdAuthority dialogRequestIdAuthority) throws Exception {

        this.microphone = AudioCapture.getAudioHardware(AUDIO_TYPE.getAudioFormat(),
                new MicrophoneLineFactory());
        this.player = audioFactory.getAudioPlayer(this);
        this.player.registerAlexaSpeechListener(this);
        this.dialogRequestIdAuthority = dialogRequestIdAuthority;
        speechRequestAudioPlayerPauseController =
                new SpeechRequestAudioPlayerPauseController(player);

        expectSpeechListeners = new HashSet<ExpectSpeechListener>(
                Arrays.asList(listenHandler, speechRequestAudioPlayerPauseController));
        dependentQueue = new LinkedBlockingDeque<>();

        independentQueue = new LinkedBlockingDeque<>();

        DirectiveEnqueuer directiveEnqueuer =
                new DirectiveEnqueuer(dialogRequestIdAuthority, dependentQueue, independentQueue);

        avsClient = avsClientFactory.getAVSClient(directiveEnqueuer, this);

        alertManager = alarmFactory.getAlertManager(this, this, AlertsFileDataStore.getInstance());

        // Ensure that we have attempted to finish loading all alarms from file before sending
        // synchronize state
        alertManager.loadFromDisk(new ResultListener() {
            @Override
            public void onSuccess() {
                sendSynchronizeStateEvent();
            }

            @Override
            public void onFailure() {
                sendSynchronizeStateEvent();
            }
        });

        // ensure we notify AVS of playbackStopped on app exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                player.stop();
                avsClient.shutdown();
            }
        });

        dependentDirectiveThread =
                new BlockableDirectiveThread(dependentQueue, this, "DependentDirectiveThread");
        independentDirectiveThread =
                new BlockableDirectiveThread(independentQueue, this, "IndependentDirectiveThread");

        lastUserInteractionTimestampSeconds =
                new AtomicLong(System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
        scheduledExecutor.scheduleAtFixedRate(new UserInactivityReport(),
                USER_INACTIVITY_REPORT_PERIOD_HOURS, USER_INACTIVITY_REPORT_PERIOD_HOURS,
                TimeUnit.HOURS);
    }

    public void startHandlingDirectives() {
        dependentDirectiveThread.start();
        independentDirectiveThread.start();
    }

    public void sendSynchronizeStateEvent() {
        sendRequest(RequestFactory.createSystemSynchronizeStateEvent(player.getPlaybackState(),
                player.getSpeechState(), alertManager.getState(), player.getVolumeState()));
    }

    @Override
    public void onAccessTokenReceived(String accessToken) {
        avsClient.setAccessToken(accessToken);
    }

    // start the recording process and send to server
    // takes an optional RMS callback and an optional request callback
    public void startRecording(RecordingRMSListener rmsListener, RequestListener requestListener) {
        try {
            String dialogRequestId = dialogRequestIdAuthority.createNewDialogRequestId();

            RequestBody body = RequestFactory.createSpeechRegonizerRecognizeRequest(dialogRequestId,
                    PROFILE, FORMAT, player.getPlaybackState(), player.getSpeechState(),
                    alertManager.getState(), player.getVolumeState());

            dependentQueue.clear();

            InputStream inputStream = microphone.getAudioInputStream(this, rmsListener);

            avsClient.sendEvent(body, inputStream, requestListener, AUDIO_TYPE);

            speechRequestAudioPlayerPauseController.startSpeechRequest();

        } catch (Exception e) {
            player.playMp3FromResource(ERROR_SOUND);
            requestListener.onRequestError(e);
        }
    }

    public void handlePlaybackAction(PlaybackAction action) {
        switch (action) {
            case PLAY:
                if (alertManager.hasActiveAlerts()) {
                    alertManager.stopActiveAlert();
                } else {
                    sendRequest(RequestFactory.createPlaybackControllerPlayEvent(
                            player.getPlaybackState(), player.getSpeechState(),
                            alertManager.getState(), player.getVolumeState()));
                }
                break;
            case PAUSE:
                if (alertManager.hasActiveAlerts()) {
                    alertManager.stopActiveAlert();
                } else {
                    sendRequest(RequestFactory.createPlaybackControllerPauseEvent(
                            player.getPlaybackState(), player.getSpeechState(),
                            alertManager.getState(), player.getVolumeState()));
                }
                break;
            case PREVIOUS:
                sendRequest(RequestFactory.createPlaybackControllerPreviousEvent(
                        player.getPlaybackState(), player.getSpeechState(), alertManager.getState(),
                        player.getVolumeState()));
                break;
            case NEXT:
                sendRequest(RequestFactory.createPlaybackControllerNextEvent(
                        player.getPlaybackState(), player.getSpeechState(), alertManager.getState(),
                        player.getVolumeState()));
                break;
            default:
                log.error("Failed to handle playback action");
        }
    }

    public void sendRequest(RequestBody body) {
        eventRunning = true;
        try {
            avsClient.sendEvent(body);
        } catch (Exception e) {
            log.error("Failed to send request", e);
        }
        eventRunning = false;
    }

    public boolean eventRunning() {
        return eventRunning;
    }

    @Override
    public synchronized void dispatch(Directive directive) {
        String directiveNamespace = directive.getNamespace();

        String directiveName = directive.getName();
        log.info("Handling directive: {}.{}", directiveNamespace, directiveName);
        if (dialogRequestIdAuthority.isCurrentDialogRequestId(directive.getDialogRequestId())) {
            speechRequestAudioPlayerPauseController.dispatchDirective();
        }
        try {
            if (directiveNamespace.equals(AVSAPIConstants.SpeechRecognizer.NAMESPACE)) {
                handleSpeechRecognizerDirective(directive);
            } else if (directiveNamespace.equals(AVSAPIConstants.SpeechSynthesizer.NAMESPACE)) {
                handleSpeechSynthesizerDirective(directive);
            } else if (directiveNamespace.equals(AVSAPIConstants.AudioPlayer.NAMESPACE)) {
                handleAudioPlayerDirective(directive);
            } else if (directiveNamespace.equals(AVSAPIConstants.Alerts.NAMESPACE)) {
                handleAlertsDirective(directive);
            } else if (directiveNamespace.equals(AVSAPIConstants.Speaker.NAMESPACE)) {
                handleSpeakerDirective(directive);
            } else if (directiveNamespace.equals(AVSAPIConstants.System.NAMESPACE)) {
                handleSystemDirective(directive);
            } else {
                throw new DirectiveHandlingException(ExceptionType.UNSUPPORTED_OPERATION,
                        "No device side component to handle the directive.");
            }
        } catch (DirectiveHandlingException e) {
            sendExceptionEncounteredEvent(directive.getRawMessage(), e.getType(), e);
        } catch (Exception e) {
            sendExceptionEncounteredEvent(directive.getRawMessage(), ExceptionType.INTERNAL_ERROR,
                    e);
            throw e;
        }

    }

    private void sendExceptionEncounteredEvent(String directiveJson, ExceptionType type,
            Exception e) {
        sendRequest(RequestFactory.createSystemExceptionEncounteredEvent(directiveJson, type,
                e.getMessage(), player.getPlaybackState(), player.getSpeechState(),
                alertManager.getState(), player.getVolumeState()));
        log.error("{} error handling directive: {}", type, directiveJson, e);
    }

    private void handleAudioPlayerDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (directiveName.equals(AVSAPIConstants.AudioPlayer.Directives.Play.NAME)) {
            player.handlePlay((Play) directive.getPayload());
        } else if (directiveName.equals(AVSAPIConstants.AudioPlayer.Directives.Stop.NAME)) {
            player.handleStop();
        } else if (directiveName.equals(AVSAPIConstants.AudioPlayer.Directives.ClearQueue.NAME)) {
            player.handleClearQueue((ClearQueue) directive.getPayload());
        }

    }

    private void handleSpeechSynthesizerDirective(Directive directive)
            throws DirectiveHandlingException {
        if (directive.getName().equals(AVSAPIConstants.SpeechSynthesizer.Directives.Speak.NAME)) {
            player.handleSpeak((Speak) directive.getPayload());
        }
    }

    private void handleSpeechRecognizerDirective(Directive directive) {
        if (directive
                .getName()
                .equals(AVSAPIConstants.SpeechRecognizer.Directives.ExpectSpeech.NAME)) {

            // If your device cannot handle automatically starting to listen, you must
            // implement a listen timeout event, as described here:
            // https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/rest/speechrecognizer-listentimeout-request
            notifyExpectSpeechDirective();
        }
    }

    private void handleAlertsDirective(Directive directive) {
        String directiveName = directive.getName();
        if (directiveName.equals(AVSAPIConstants.Alerts.Directives.SetAlert.NAME)) {
            SetAlert payload = (SetAlert) directive.getPayload();
            String alertToken = payload.getToken();
            ZonedDateTime scheduledTime = payload.getScheduledTime();
            AlertType type = payload.getType();

            if (alertManager.hasAlert(alertToken)) {
                AlertScheduler scheduler = alertManager.getScheduler(alertToken);
                if (scheduler.getAlert().getScheduledTime().equals(scheduledTime)) {
                    return;
                } else {
                    scheduler.cancel();
                }
            }

            Alert alert = new Alert(alertToken, type, scheduledTime);
            alertManager.add(alert);
        } else if (directiveName.equals(AVSAPIConstants.Alerts.Directives.DeleteAlert.NAME)) {
            DeleteAlert payload = (DeleteAlert) directive.getPayload();
            alertManager.delete(payload.getToken());
        }
    }

    private void handleSpeakerDirective(Directive directive) {
        String directiveName = directive.getName();
        if (directiveName.equals(AVSAPIConstants.Speaker.Directives.SetVolume.NAME)) {
            player.handleSetVolume((VolumePayload) directive.getPayload());
        } else if (directiveName.equals(AVSAPIConstants.Speaker.Directives.AdjustVolume.NAME)) {
            player.handleAdjustVolume((VolumePayload) directive.getPayload());
        } else if (directiveName.equals(AVSAPIConstants.Speaker.Directives.SetMute.NAME)) {
            player.handleSetMute((SetMute) directive.getPayload());
        }
    }

    private void handleSystemDirective(Directive directive) {
        if (directive
                .getName()
                .equals(AVSAPIConstants.System.Directives.ResetUserInactivity.NAME)) {
            onUserActivity();
        }
    }

    private void notifyExpectSpeechDirective() {
        for (ExpectSpeechListener listener : expectSpeechListeners) {
            listener.onExpectSpeechDirective();
        }
    }

    public void stopRecording() {
        speechRequestAudioPlayerPauseController.finishedListening();
        microphone.stopCapture();
    }

    // audio state callback for when recording has started
    @Override
    public void recordingStarted() {
        player.playMp3FromResource(START_SOUND);
    }

    // audio state callback for when recording has completed
    @Override
    public void recordingCompleted() {
        player.playMp3FromResource(END_SOUND);
    }

    public boolean isSpeaking() {
        return player.isSpeaking();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void onAlertStarted(String alertToken) {
        sendRequest(RequestFactory.createAlertsAlertStartedEvent(alertToken));

        if (player.isSpeaking()) {
            sendRequest(RequestFactory.createAlertsAlertEnteredBackgroundEvent(alertToken));
        } else {
            sendRequest(RequestFactory.createAlertsAlertEnteredForegroundEvent(alertToken));
        }
    }

    @Override
    public void onAlertStopped(String alertToken) {
        sendRequest(RequestFactory.createAlertsAlertStoppedEvent(alertToken));
    }

    @Override
    public void onAlertSet(String alertToken, boolean success) {
        sendRequest(RequestFactory.createAlertsSetAlertEvent(alertToken, success));
    }

    @Override
    public void onAlertDelete(String alertToken, boolean success) {
        sendRequest(RequestFactory.createAlertsDeleteAlertEvent(alertToken, success));
    }

    @Override
    public void startAlert(String alertToken) {
        player.startAlert();
    }

    @Override
    public void stopAlert(String alertToken) {
        if (!alertManager.hasActiveAlerts()) {
            player.stopAlert();
        }
    }

    public void processingFinished() {
        speechRequestAudioPlayerPauseController
                .speechRequestProcessingFinished(dependentQueue.size());
    }

    @Override
    public void onAlexaSpeechStarted() {
        dependentDirectiveThread.block();

        if (alertManager.hasActiveAlerts()) {
            for (String alertToken : alertManager.getActiveAlerts()) {
                sendRequest(RequestFactory.createAlertsAlertEnteredBackgroundEvent(alertToken));
            }
        }
    }

    @Override
    public void onAlexaSpeechFinished() {
        dependentDirectiveThread.unblock();

        if (alertManager.hasActiveAlerts()) {
            for (String alertToken : alertManager.getActiveAlerts()) {
                sendRequest(RequestFactory.createAlertsAlertEnteredForegroundEvent(alertToken));
            }
        }
    }

    @Override
    public void onParsingFailed(String unparseable) {
        String message = "Failed to parse message from AVS";
        sendRequest(RequestFactory.createSystemExceptionEncounteredEvent(unparseable,
                ExceptionType.UNEXPECTED_INFORMATION_RECEIVED, message, player.getPlaybackState(),
                player.getSpeechState(), alertManager.getState(), player.getVolumeState()));
    }

    @Override
    public void onUserActivity() {
        lastUserInteractionTimestampSeconds
                .set(System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
    }

    private class UserInactivityReport implements Runnable {

        @Override
        public void run() {
            sendRequest(RequestFactory.createSystemUserInactivityReportEvent(
                    (System.currentTimeMillis() / MILLISECONDS_PER_SECOND)
                            - lastUserInteractionTimestampSeconds.get()));
        }
    }
}
