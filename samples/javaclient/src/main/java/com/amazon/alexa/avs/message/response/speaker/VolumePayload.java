package com.amazon.alexa.avs.message.response.speaker;

import com.amazon.alexa.avs.message.Payload;

public abstract class VolumePayload extends Payload {
    private long volume;

    public final void setVolume(long volume) {
        this.volume = volume;
    }

    public final long getVolume() {
        return volume;
    }
}
