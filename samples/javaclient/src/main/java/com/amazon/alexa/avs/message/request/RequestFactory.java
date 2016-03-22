/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.message.request;

import com.amazon.alexa.avs.AVSAPIConstants;
import com.amazon.alexa.avs.SpeechProfile;
import com.amazon.alexa.avs.exception.DirectiveHandlingException.ExceptionType;
import com.amazon.alexa.avs.message.DialogRequestIdHeader;
import com.amazon.alexa.avs.message.Header;
import com.amazon.alexa.avs.message.MessageIdHeader;
import com.amazon.alexa.avs.message.Payload;
import com.amazon.alexa.avs.message.request.alerts.AlertPayload;
import com.amazon.alexa.avs.message.request.audioplayer.AudioPlayerPayload;
import com.amazon.alexa.avs.message.request.audioplayer.PlaybackFailedPayload;
import com.amazon.alexa.avs.message.request.audioplayer.PlaybackFailedPayload.ErrorType;
import com.amazon.alexa.avs.message.request.audioplayer.PlaybackStutterFinishedPayload;
import com.amazon.alexa.avs.message.request.context.AlertsStatePayload;
import com.amazon.alexa.avs.message.request.context.ComponentState;
import com.amazon.alexa.avs.message.request.context.PlaybackStatePayload;
import com.amazon.alexa.avs.message.request.context.SpeechStatePayload;
import com.amazon.alexa.avs.message.request.context.VolumeStatePayload;
import com.amazon.alexa.avs.message.request.speechrecognizer.SpeechRecognizerPayload;
import com.amazon.alexa.avs.message.request.speechsynthesizer.SpeechLifecyclePayload;
import com.amazon.alexa.avs.message.request.system.ExceptionEncounteredPayload;
import com.amazon.alexa.avs.message.request.system.UserInactivityReportPayload;

import java.util.Arrays;
import java.util.List;

public class RequestFactory {

    public interface Request {
        RequestBody withPlaybackStatePayload(PlaybackStatePayload state);
    }

    public static RequestBody createSpeechRegonizerRecognizeRequest(String dialogRequestId,
            SpeechProfile profile, String format, PlaybackStatePayload playerState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        SpeechRecognizerPayload payload = new SpeechRecognizerPayload(profile, format);
        Header header = new DialogRequestIdHeader(AVSAPIConstants.SpeechRecognizer.NAMESPACE,
                AVSAPIConstants.SpeechRecognizer.Events.Recognize.NAME, dialogRequestId);
        Event event = new Event(header, payload);
        return createRequestWithAllState(event, playerState, speechState, alertState, volumeState);
    }

    public static RequestBody createAudioPlayerPlaybackStartedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackStarted.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackNearlyFinishedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.PlaybackNearlyFinished.NAME, streamToken,
                offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackStutterStartedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.PlaybackStutterStarted.NAME, streamToken,
                offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackStutterFinishedEvent(String streamToken,
            long offsetInMilliseconds, long stutterDurationInMilliseconds) {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackStutterFinished.NAME);
        Event event = new Event(header, new PlaybackStutterFinishedPayload(streamToken,
                offsetInMilliseconds, stutterDurationInMilliseconds));
        return new RequestBody(event);
    }

    public static RequestBody createAudioPlayerPlaybackFinishedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackFinished.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackStoppedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackStopped.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackPausedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackPaused.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackResumedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackResumed.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerPlaybackQueueClearedEvent() {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackQueueCleared.NAME);
        Event event = new Event(header, new Payload());
        return new RequestBody(event);
    }

    public static RequestBody createAudioPlayerPlaybackFailedEvent(String streamToken,
            PlaybackStatePayload playbackStatePayload, ErrorType errorType) {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackFailed.NAME);
        Event event = new Event(header,
                new PlaybackFailedPayload(streamToken, playbackStatePayload, errorType));
        return new RequestBody(event);
    }

    public static RequestBody createAudioPlayerProgressReportDelayElapsedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.ProgressReportDelayElapsed.NAME, streamToken,
                offsetInMilliseconds);
    }

    public static RequestBody createAudioPlayerProgressReportIntervalElapsedEvent(
            String streamToken, long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.ProgressReportIntervalElapsed.NAME, streamToken,
                offsetInMilliseconds);
    }

    private static RequestBody createAudioPlayerEvent(String name, String streamToken,
            long offsetInMilliseconds) {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE, name);
        Payload payload = new AudioPlayerPayload(streamToken, offsetInMilliseconds);
        Event event = new Event(header, payload);
        return new RequestBody(event);
    }

    public static RequestBody createPlaybackControllerNextEvent(PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.NextCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    public static RequestBody createPlaybackControllerPreviousEvent(
            PlaybackStatePayload playbackState, SpeechStatePayload speechState,
            AlertsStatePayload alertState, VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.PreviousCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    public static RequestBody createPlaybackControllerPlayEvent(PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.PlayCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    public static RequestBody createPlaybackControllerPauseEvent(PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.PauseCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    private static RequestBody createPlaybackControllerEvent(String name,
            PlaybackStatePayload playbackState, SpeechStatePayload speechState,
            AlertsStatePayload alertState, VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSAPIConstants.PlaybackController.NAMESPACE, name);
        Event event = new Event(header, new Payload());
        return createRequestWithAllState(event, playbackState, speechState, alertState,
                volumeState);
    }

    public static RequestBody createSpeechSynthesizerSpeechStartedEvent(String speakToken) {
        return createSpeechSynthesizerEvent(
                AVSAPIConstants.SpeechSynthesizer.Events.SpeechStarted.NAME, speakToken);
    }

    public static RequestBody createSpeechSynthesizerSpeechFinishedEvent(String speakToken) {
        return createSpeechSynthesizerEvent(
                AVSAPIConstants.SpeechSynthesizer.Events.SpeechFinished.NAME, speakToken);
    }

    private static RequestBody createSpeechSynthesizerEvent(String name, String speakToken) {
        Header header = new MessageIdHeader(AVSAPIConstants.SpeechSynthesizer.NAMESPACE, name);
        Event event = new Event(header, new SpeechLifecyclePayload(speakToken));
        return new RequestBody(event);
    }

    public static RequestBody createAlertsSetAlertEvent(String alertToken, boolean success) {
        if (success) {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.SetAlertSucceeded.NAME,
                    alertToken);
        } else {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.SetAlertFailed.NAME, alertToken);
        }
    }

    public static RequestBody createAlertsDeleteAlertEvent(String alertToken, boolean success) {
        if (success) {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.DeleteAlertSucceeded.NAME,
                    alertToken);
        } else {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.DeleteAlertFailed.NAME,
                    alertToken);
        }
    }

    public static RequestBody createAlertsAlertStartedEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertStarted.NAME, alertToken);
    }

    public static RequestBody createAlertsAlertStoppedEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertStopped.NAME, alertToken);
    }

    public static RequestBody createAlertsAlertEnteredForegroundEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertEnteredForeground.NAME,
                alertToken);
    }

    public static RequestBody createAlertsAlertEnteredBackgroundEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertEnteredBackground.NAME,
                alertToken);
    }

    private static RequestBody createAlertsEvent(String name, String alertToken) {
        Header header = new MessageIdHeader(AVSAPIConstants.Alerts.NAMESPACE, name);
        Payload payload = new AlertPayload(alertToken);
        Event event = new Event(header, payload);
        return new RequestBody(event);
    }

    public static RequestBody createSpeakerVolumeChangedEvent(long volume, boolean muted) {
        return createSpeakerEvent(AVSAPIConstants.Speaker.Events.VolumeChanged.NAME, volume, muted);
    }

    public static RequestBody createSpeakerMuteChangedEvent(long volume, boolean muted) {
        return createSpeakerEvent(AVSAPIConstants.Speaker.Events.MuteChanged.NAME, volume, muted);
    }

    public static RequestBody createSpeakerEvent(String name, long volume, boolean muted) {
        Header header = new MessageIdHeader(AVSAPIConstants.Speaker.NAMESPACE, name);

        Event event = new Event(header, new VolumeStatePayload(volume, muted));
        return new RequestBody(event);
    }

    public static RequestBody createSystemSynchronizeStateEvent(PlaybackStatePayload playerState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSAPIConstants.System.NAMESPACE,
                AVSAPIConstants.System.Events.SynchronizeState.NAME);
        Event event = new Event(header, new Payload());
        return createRequestWithAllState(event, playerState, speechState, alertState, volumeState);
    }

    public static RequestBody createSystemExceptionEncounteredEvent(String directiveJson,
            ExceptionType type, String message, PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSAPIConstants.System.NAMESPACE,
                AVSAPIConstants.System.Events.ExceptionEncountered.NAME);

        Event event =
                new Event(header, new ExceptionEncounteredPayload(directiveJson, type, message));

        return createRequestWithAllState(event, playbackState, speechState, alertState,
                volumeState);
    }

    public static RequestBody createSystemUserInactivityReportEvent(long inactiveTimeInSeconds) {
        Header header = new MessageIdHeader(AVSAPIConstants.System.NAMESPACE,
                AVSAPIConstants.System.Events.UserInactivityReport.NAME);
        Event event = new Event(header, new UserInactivityReportPayload(inactiveTimeInSeconds));
        return new RequestBody(event);
    }

    private static RequestBody createRequestWithAllState(Event event,
            PlaybackStatePayload playbackState, SpeechStatePayload speechState,
            AlertsStatePayload alertState, VolumeStatePayload volumeState) {
        List<ComponentState> context =
                Arrays.asList(ComponentStateFactory.createPlaybackState(playbackState),
                        ComponentStateFactory.createSpeechState(speechState),
                        ComponentStateFactory.createAlertState(alertState),
                        ComponentStateFactory.createVolumeState(volumeState));
        return new ContextEventRequestBody(context, event);
    }
}
