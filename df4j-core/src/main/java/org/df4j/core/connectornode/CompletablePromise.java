package org.df4j.core.connectornode;

import org.df4j.core.Feeder;
import org.df4j.core.Port;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> extends CompletableFuture<R> implements Port<R>, Feeder<R> {

    @Override
    public void onNext(R message) {
        super.complete(message);
    }

    @Override
    public void onError(Throwable ex) {
        super.completeExceptionally(ex);
    }

    @Override
    public void subscribe(Port<? super R> subscriber) {
        new ScalarSubscription(subscriber);
    }

    /**
     * Returns a new CompletableFuture that is already completed with
     * the given value.
     *
     * @param value the value
     * @param <U> the type of the value
     * @return the completed CompletableFuture
     */
    public static <U> CompletablePromise<U> completedPromise(U value) {
        CompletablePromise<U> result = new CompletablePromise<>();
        result.complete(value);
        return result;
    }

    public void onComplete() {
        super.complete(null);
    }


    class ScalarSubscription implements BiConsumer<R, Throwable> {

        private final Port<? super R> subscriber;

        public <S extends Port<? super R>> ScalarSubscription(S subscriber) {
            this.subscriber = subscriber;
            CompletablePromise.this.whenComplete(this);
        }

        @Override
        public void accept(R r, Throwable throwable) {
            if (throwable != null) {
                subscriber.onError(throwable);
            } else {
                subscriber.onNext(r);
            }
        }
    }
}
