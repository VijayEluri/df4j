package org.df4j.core.boundconnector.messagestream;

import org.reactivestreams.Subscription;
import org.df4j.core.tasknode.AsyncProc;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 *
 * @param <M> type of tokens
 */
public class StreamOutput<M> extends AsyncProc.Lock implements StreamPublisher<M>, StreamSubscriber<M> {
    protected AsyncProc actor;
    protected Set<SimpleSubscriptionImpl> subscriptions = new HashSet<>();

    public StreamOutput(AsyncProc actor) {
        actor.super(false);
        this.actor = actor;
    }

    @Override
    public SimpleSubscriptionImpl subscribe(StreamSubscriber<M> subscriber) {
        SimpleSubscriptionImpl newSubscription = new SimpleSubscriptionImpl(subscriber);
        subscriptions.add(newSubscription);
        return newSubscription;
    }

    public synchronized void close() {
        subscriptions = null;
        super.turnOff();
    }

    public synchronized boolean closed() {
        return super.isBlocked();
    }

    public void forEachSubscription(Consumer<? super SimpleSubscriptionImpl> operator) {
        if (closed()) {
            return; // completed already
        }
        subscriptions.forEach(operator);
    }

    @Override
    public void post(M item) {
        if (item == null) {
            throw new NullPointerException();
        }
        forEachSubscription((subscription) -> subscription.post(item));
    }

    @Override
    public void postFailure(Throwable throwable) {
        forEachSubscription((subscription) -> subscription.postFailure(throwable));
    }

    @Override
    public synchronized void complete() {
        forEachSubscription(SimpleSubscriptionImpl::complete);
    }

    class SimpleSubscriptionImpl implements Subscription {
        protected StreamSubscriber<? super M> subscriber;
        private volatile boolean closed = false;

        public SimpleSubscriptionImpl(StreamSubscriber<? super M> subscriber) {
            this.subscriber = subscriber;
        }

        public void post(M message) {
            subscriber.post(message);
        }

        public void postFailure(Throwable throwable) {
            subscriber.postFailure(throwable);
            cancel();
        }

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        public void complete() {
            if (subscriber == null) {
                return;
            }
            subscriber.complete();
            subscriber = null;
        }

        @Override
        public void request(long n) {}

        /**
         * subscription closed by request of subscriber
         */
        public void cancel() {
            synchronized(StreamOutput.this) {
                if (closed) {
                    return;
                }
                closed = true;
                subscriptions.remove(this);
            }
        }
    }

}
