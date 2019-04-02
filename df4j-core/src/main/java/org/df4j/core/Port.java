/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.df4j.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * scalar inlet for messages
 *
 * @param <T> the type of the message
 */
@FunctionalInterface
public interface Port<T> extends BiConsumer<T, Throwable> {

    /**
     * Data notification sent by the {@link Feeder} in response to requests to {@link Subscription#request(long)}.
     *
     * @param t the element signaled
     */
    void onNext(T t);

    /**
     * callback for
     */
    default void onTimout() {}


    /**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     *
     * @param ex the completion exception
     */
    default void onError(Throwable ex) {}

    /**
     * successful end of stream
     */
    default void onComplete() {}

    /**
     * to pass data from  {@link CompletableFuture} to ScalarSubscriber using     *
     * <pre>
     *     completableFuture.whenComplete(scalarSubscriber)
     * </pre>
     * @param r
     * @param throwable
     */
    @Override
    default void accept(T r, Throwable throwable) {
        if (throwable != null) {
            onError(throwable);
        } else {
            onNext(r);
        }
    }
}
