/**
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.AVSAudioPlayer.AlexaSpeechListener;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * This class keeps track of running speech requests and whether the device is listening/speaking to
 * appropriately manage the pause state of the player.
 */
public class SpeechRequestAudioPlayerPauseController
        implements AlexaSpeechListener, ExpectSpeechListener {
    private final AVSAudioPlayer audioPlayer;
    private Optional<CountDownLatch> outstandingDirectiveCount = Optional.empty();
    private Optional<Thread> resumeAudioThread = Optional.empty();
    private Optional<CountDownLatch> alexaSpeaking = Optional.empty();
    private Optional<CountDownLatch> alexaListening = Optional.empty();
    boolean speechRequestRunning = false;

    public SpeechRequestAudioPlayerPauseController(AVSAudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        audioPlayer.registerAlexaSpeechListener(this);
    }

    /**
     * Called when the starting a speech request to alexa voice service
     */
    public void startSpeechRequest() {
        alexaListening = Optional.of(new CountDownLatch(1));
        audioPlayer.interruptAllAlexaOutput();
        resumeAudioThread.ifPresent(t -> t.interrupt());
        speechRequestRunning = true;
    }

    /**
     * Called when finished Listening
     */
    public void finishedListening() {
        alexaListening.ifPresent(c -> c.countDown());
        if (!speechRequestRunning) {
            audioPlayer.resumeAllAlexaOutput();
        }
    }

    /**
     * Called each time a directive is dispatched
     */
    public void dispatchDirective() {
        outstandingDirectiveCount.ifPresent(c -> c.countDown());
    }

    @Override
    public void onAlexaSpeechStarted() {
        alexaSpeaking = Optional.of(new CountDownLatch(1));
    }

    @Override
    public void onAlexaSpeechFinished() {
        alexaSpeaking.ifPresent(c -> c.countDown());
        if (!speechRequestRunning) {
            audioPlayer.resumeAllAlexaOutput();
        }
    }

    @Override
    public void onExpectSpeechDirective() {
        alexaListening = Optional.of(new CountDownLatch(1));
    }

    /**
     * A speech request has been finished processing
     *
     * @param directiveCount
     *            the number of outstanding directives that correspond to the speech request that
     *            just finished
     */
    public void speechRequestProcessingFinished(int directiveCount) {
        resumeAudioThread.ifPresent(t -> t.interrupt());
        outstandingDirectiveCount = Optional.of(new CountDownLatch(directiveCount));
        resumeAudioThread = Optional.of(new Thread() {

            boolean isInterrupted = false;

            @Override
            public void run() {
                outstandingDirectiveCount.ifPresent(c -> awaitOnLatch(c));
                if (alexaListening.isPresent() || alexaSpeaking.isPresent()) {
                    alexaSpeaking.ifPresent(c -> awaitOnLatch(c));
                    alexaListening.ifPresent(c -> awaitOnLatch(c));
                }
                if (!isInterrupted) {
                    speechRequestRunning = false;
                    audioPlayer.resumeAllAlexaOutput();
                }

            }

            private void awaitOnLatch(CountDownLatch latch) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    // If another speech request is kicked off while we're processing the
                    // current request we expect this thread to be interrupted
                    isInterrupted = true;
                }
            }

        });
        resumeAudioThread.ifPresent(t -> t.start());

    }

}
