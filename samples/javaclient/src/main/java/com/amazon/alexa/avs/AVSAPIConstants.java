/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.amazon.alexa.avs;

public final class AVSAPIConstants {

    public static final class AudioPlayer {
        public static final String NAMESPACE = AudioPlayer.class.getSimpleName();

        public static final class Events {
            public static final class PlaybackStarted {
                public static final String NAME = PlaybackStarted.class.getSimpleName();
            }

            public static final class PlaybackNearlyFinished {
                public static final String NAME = PlaybackNearlyFinished.class.getSimpleName();
            }

            public static final class PlaybackStutterStarted {
                public static final String NAME = PlaybackStutterStarted.class.getSimpleName();
            }

            public static final class PlaybackStutterFinished {
                public static final String NAME = PlaybackStutterFinished.class.getSimpleName();
            }

            public static final class PlaybackFinished {
                public static final String NAME = PlaybackFinished.class.getSimpleName();
            }

            public static final class PlaybackFailed {
                public static final String NAME = PlaybackFailed.class.getSimpleName();
            }

            public static final class PlaybackStopped {
                public static final String NAME = PlaybackStopped.class.getSimpleName();
            }

            public static final class PlaybackPaused {
                public static final String NAME = PlaybackPaused.class.getSimpleName();
            }

            public static final class PlaybackResumed {
                public static final String NAME = PlaybackResumed.class.getSimpleName();
            }

            public static final class PlaybackQueueCleared {
                public static final String NAME = PlaybackQueueCleared.class.getSimpleName();
            }

            public static final class ProgressReportDelayElapsed {
                public static final String NAME = ProgressReportDelayElapsed.class.getSimpleName();
            }

            public static final class ProgressReportIntervalElapsed {
                public static final String NAME =
                        ProgressReportIntervalElapsed.class.getSimpleName();
            }

            public static final class PlaybackState {
                public static final String NAME = PlaybackState.class.getSimpleName();
            }
        }

        public static final class Directives {

            public static final class Play {
                public static final String NAME = Play.class.getSimpleName();
            }

            public static final class Stop {
                public static final String NAME = Stop.class.getSimpleName();
            }

            public static final class ClearQueue {
                public static final String NAME = ClearQueue.class.getSimpleName();
            }
        }
    }

    public static final class PlaybackController {
        public static final String NAMESPACE = PlaybackController.class.getSimpleName();

        public static final class Events {

            public static final class NextCommandIssued {
                public static final String NAME = NextCommandIssued.class.getSimpleName();
            }

            public static final class PreviousCommandIssued {
                public static final String NAME = PreviousCommandIssued.class.getSimpleName();
            }

            public static final class PlayCommandIssued {
                public static final String NAME = PlayCommandIssued.class.getSimpleName();
            }

            public static final class PauseCommandIssued {
                public static final String NAME = PauseCommandIssued.class.getSimpleName();
            }
        }
    }

    public static final class SpeechSynthesizer {
        public static final String NAMESPACE = SpeechSynthesizer.class.getSimpleName();

        public static final class Events {

            public static final class SpeechStarted {
                public static final String NAME = SpeechStarted.class.getSimpleName();
            }

            public static final class SpeechFinished {
                public static final String NAME = SpeechFinished.class.getSimpleName();
            }

            public static final class SpeechState {
                public static final String NAME = SpeechState.class.getSimpleName();
            }
        }

        public static final class Directives {

            public static final class Speak {
                public static final String NAME = Speak.class.getSimpleName();
            }
        }
    }

    public static final class SpeechRecognizer {
        public static final String NAMESPACE = SpeechRecognizer.class.getSimpleName();

        public static final class Events {

            public static final class Recognize {
                public static final String NAME = Recognize.class.getSimpleName();
            }

            public static final class ExpectSpeechTimedOut {
                public static final String NAME = ExpectSpeechTimedOut.class.getSimpleName();
            }
        }

        public static final class Directives {

            public static final class ExpectSpeech {
                public static final String NAME = ExpectSpeech.class.getSimpleName();
            }

            public static final class StopCapture {
                public static final String NAME = StopCapture.class.getSimpleName();
            }

            public static final class RequestProcessingStarted {
                public static final String NAME = RequestProcessingStarted.class.getSimpleName();
            }
        }
    }

    public static class Alerts {
        public static final String NAMESPACE = Alerts.class.getSimpleName();

        public static final class Events {

            public static final class SetAlertSucceeded {
                public static final String NAME = SetAlertSucceeded.class.getSimpleName();
            }

            public static final class SetAlertFailed {
                public static final String NAME = SetAlertFailed.class.getSimpleName();
            }

            public static final class DeleteAlertSucceeded {
                public static final String NAME = DeleteAlertSucceeded.class.getSimpleName();
            }

            public static final class DeleteAlertFailed {
                public static final String NAME = DeleteAlertFailed.class.getSimpleName();
            }

            public static final class AlertStarted {
                public static final String NAME = AlertStarted.class.getSimpleName();
            }

            public static final class AlertStopped {
                public static final String NAME = AlertStopped.class.getSimpleName();
            }

            public static final class AlertsState {
                public static final String NAME = AlertsState.class.getSimpleName();
            }

            public static final class AlertEnteredForeground {
                public static final String NAME = AlertEnteredForeground.class.getSimpleName();
            }

            public static final class AlertEnteredBackground {
                public static final String NAME = AlertEnteredBackground.class.getSimpleName();
            }
        }

        public static final class Directives {

            public static final class SetAlert {
                public static final String NAME = SetAlert.class.getSimpleName();
            }

            public static final class DeleteAlert {
                public static final String NAME = DeleteAlert.class.getSimpleName();
            }
        }
    }

    public static final class Speaker {

        public static final String NAMESPACE = Speaker.class.getSimpleName();

        public static final class Events {

            public static final class VolumeChanged {
                public static final String NAME = VolumeChanged.class.getSimpleName();
            }

            public static final class MuteChanged {
                public static final String NAME = MuteChanged.class.getSimpleName();
            }

            public static final class VolumeState {
                public static final String NAME = VolumeState.class.getSimpleName();
            }
        }

        public static final class Directives {

            public static final class SetVolume {
                public static final String NAME = SetVolume.class.getSimpleName();
            }

            public static final class AdjustVolume {
                public static final String NAME = AdjustVolume.class.getSimpleName();
            }

            public static final class SetMute {
                public static final String NAME = SetMute.class.getSimpleName();
            }
        }
    }

    public static final class System {
        public static final String NAMESPACE = System.class.getSimpleName();

        public static final class Exception {
            public static final String NAME = Exception.class.getSimpleName();
        }

        public static final class Events {

            public static final class SynchronizeState {
                public static final String NAME = SynchronizeState.class.getSimpleName();
            }

            public static final class ExceptionEncountered {
                public static final String NAME = ExceptionEncountered.class.getSimpleName();
            }

            public static final class UserInactivityReport {
                public static final String NAME = UserInactivityReport.class.getSimpleName();
            }
        }

        public static final class Directives {

            public static final class ResetUserInactivity {
                public static final String NAME = ResetUserInactivity.class.getSimpleName();
            }
        }
    }
}
