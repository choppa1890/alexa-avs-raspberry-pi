/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.message.response.audioplayer;

import com.amazon.alexa.avs.message.Payload;

public final class ClearQueue extends Payload {

    public enum ClearBehavior {
        CLEAR_ENQUEUED,
        CLEAR_ALL;
    }

    private ClearBehavior clearBehavior;

    public ClearBehavior getClearBehavior() {
        return clearBehavior;
    }

    public void setClearBehavior(String clearBehavior) {
        this.clearBehavior = ClearBehavior.valueOf(clearBehavior);
    }
}
