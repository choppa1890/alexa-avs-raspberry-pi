package com.amazon.alexa.avs.message.response.speaker;

import com.amazon.alexa.avs.message.Payload;

public class SetMute extends Payload {
    private boolean mute;

    public final void setMute(boolean mute) {
        this.mute = mute;
    }

    public final boolean getMute() {
        return mute;
    }
}
