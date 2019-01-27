package org.df4j.core.boundconnector.messagescalar;

import org.reactivestreams.Subscription;
import org.df4j.core.simplenode.messagescalar.CompletablePromise;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * this is a source of a single deferred value
 *
 * it could be named "Promise" also
 *
 * @param <T> the published item type
 */
@FunctionalInterface
public interface ScalarPublisher<T> {
    /**
     * Adds the given ScalarSubscriber if possible.  If already
     * subscribed, or the attempt to subscribe fails due to policy
     * violations or errors, the Subscriber's {@code postFailure}
     * method is invoked with an {@link IllegalStateException}.
     *
     * @param subscriber the subscriber
     * @return the subscriber argument
     */
//    <S extends ScalarSubscriber<? super T>> S subscribe(S subscriber);
    Subscription subscribe(ScalarSubscriber<T> subscriber);

    /**
     * to access the published result in synchronous way
     *
     * @return Future subscribed to this publisher
     */
    default Future<T> asFuture() {
        CompletablePromise<T> subscriber = new CompletablePromise<>();
        subscribe(subscriber);
        return subscriber;
    }

}
