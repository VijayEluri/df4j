package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.messagestream.StreamSubscriber;
import org.df4j.core.boundconnector.permitstream.Semafor;
import org.df4j.core.tasknode.AsyncProc;
import org.reactivestreams.Subscription;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * serves multiple subscribers
 * demonstrates usage of class AsyncProc.Semafor for handling back pressure
 *
 * An equivalent to java.util.concurrent.SubmissionPublisher
 *
 * @param <M> the type of broadcasted values
 */
public class ReactiveOutput<M> extends AsyncProc.Lock implements ReactivePublisher<M>, StreamSubscriber<M> {
    protected AsyncProc actor;
    protected Set<SimpleReactiveSubscriptionImpl> subscriptions = new HashSet<>();

    public ReactiveOutput(AsyncProc actor) {
        actor.super(false);
        this.actor = actor;
    }

    @Override
    public <S extends ReactiveSubscriber<? super M>> S subscribe(S subscriber) {
        SimpleReactiveSubscriptionImpl newSubscription = new SimpleReactiveSubscriptionImpl(subscriber);
        subscriptions.add(newSubscription);
        subscriber.onSubscribe(newSubscription);
        return subscriber;
    }

    public synchronized void close() {
        subscriptions = null;
        super.turnOff();
    }

    public synchronized boolean closed() {
        return super.isBlocked();
    }

    public void forEachSubscription(Consumer<? super SimpleReactiveSubscriptionImpl> operator) {
        if (closed()) {
            return; // completed already
        }
        subscriptions.forEach(operator);
    }

    @Override
    public void post(M item) {
        forEachSubscription((subscription) -> subscription.post(item));
    }

    public synchronized void complete() {
        forEachSubscription(SimpleReactiveSubscriptionImpl::complete);
    }

    @Override
    public void postFailure(Throwable throwable) {
        forEachSubscription((subscription) -> subscription.postFailure(throwable));
    }

    class SimpleReactiveSubscriptionImpl extends Semafor implements Subscription {
        protected ReactiveSubscriber<? super M> subscriber;
        private volatile boolean closed = false;

        public SimpleReactiveSubscriptionImpl(ReactiveSubscriber<? super M> subscriber) {
            super(ReactiveOutput.this.actor);
            if (subscriber == null) {
                throw new NullPointerException();
            }
            this.subscriber = subscriber;
        }

        public void post(M message) {
            if (isCompleted()) {
                throw new IllegalStateException("post to completed connector");
            }
            subscriber.post(message);
        }

        public void postFailure(Throwable throwable) {
            if (isCompleted()) {
                throw new IllegalStateException("postFailure to completed connector");
            }
            subscriber.postFailure(throwable);
            cancel();
        }

        /**
         * does nothing: counter decreases when a message is posted
         */
        @Override
        public void purge() {
        }

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        public void complete() {
            if (isCompleted()) {
                return;
            }
            subscriber.complete();
            subscriber = null;
        }

        private boolean isCompleted() {
            return subscriber == null;
        }

        /**
         * subscription closed by request of subscriber
         */
        public synchronized void cancel() {
            if (isCompleted()) {
                return;
            }
            subscriber = null;
            closed = true;
            subscriptions.remove(this);
            super.unRegister(); // and cannot be turned on
        }

        @Override
        public void request(long n) {
            super.release(n);
        }
    }

}
