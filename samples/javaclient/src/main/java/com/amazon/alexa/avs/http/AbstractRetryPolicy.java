/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.http;

import java.util.concurrent.Callable;

public abstract class AbstractRetryPolicy implements RetryPolicy {
    private int maxAttempts;

    public AbstractRetryPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tryCall(Callable<Void> callable, Class<? extends Throwable> exception)
            throws Exception {
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
                callable.call();
                break;
            } catch (Exception e) {
                attempts++;
                if ((exception != null) && (exception.isAssignableFrom(e.getClass()))
                        && !(attempts >= maxAttempts)) {
                    Thread.sleep(getDelay(attempts));
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Get the expected delay in milliseconds.
     *
     * @param attempts
     * @return
     */
    protected abstract long getDelay(int attempts);
}
