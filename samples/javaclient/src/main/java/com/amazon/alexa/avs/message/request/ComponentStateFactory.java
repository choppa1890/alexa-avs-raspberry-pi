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
import com.amazon.alexa.avs.message.Header;
import com.amazon.alexa.avs.message.request.context.AlertsStatePayload;
import com.amazon.alexa.avs.message.request.context.ComponentState;
import com.amazon.alexa.avs.message.request.context.PlaybackStatePayload;
import com.amazon.alexa.avs.message.request.context.SpeechStatePayload;
import com.amazon.alexa.avs.message.request.context.VolumeStatePayload;

public class ComponentStateFactory {

    public static ComponentState createPlaybackState(PlaybackStatePayload playerState) {
        return new ComponentState(new Header(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackState.NAME), playerState);
    }

    public static ComponentState createSpeechState(SpeechStatePayload speechState) {
        return new ComponentState(new Header(AVSAPIConstants.SpeechSynthesizer.NAMESPACE,
                AVSAPIConstants.SpeechSynthesizer.Events.SpeechState.NAME), speechState);
    }

    public static ComponentState createAlertState(AlertsStatePayload alertState) {
        return new ComponentState(new Header(AVSAPIConstants.Alerts.NAMESPACE,
                AVSAPIConstants.Alerts.Events.AlertsState.NAME), alertState);
    }

    public static ComponentState createVolumeState(VolumeStatePayload volumeState) {
        return new ComponentState(new Header(AVSAPIConstants.Speaker.NAMESPACE,
                AVSAPIConstants.Speaker.Events.VolumeState.NAME), volumeState);
    }
}
