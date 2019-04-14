package org.df4j.core.actor;

import org.df4j.core.SubscriptionListener;
import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * blocks when there are no active subscribers
 */
public class StreamSubscriptionBlockingQueue<T> extends Transition.Param<StreamSubscription<T>>
        implements SubscriptionListener<StreamSubscription<T>>, Publisher<T>
{
    protected StreamSubscriptionQueue<T> subscriptions =  new StreamSubscriptionQueue<>(this);

    public StreamSubscriptionBlockingQueue(AsyncProc actor) {
        actor.super();
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        subscriptions.subscribe(s);
    }

    public void onError(Throwable ex) {
        subscriptions.onError(ex);
    }

    public void onComplete() {
        subscriptions.onComplete();
        super.complete();
    }

    @Override
    public StreamSubscription<T> current() {
        return subscriptions.current();
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active subcriptions remain
     *         false otherwise
     */
    public synchronized void cancel(StreamSubscription<T> subscription) {
        subscriptions.cancel(subscription);
        if (subscriptions.noActiveSubscribers()) {
            block();
        }
    }

    @Override
    public synchronized void activate(StreamSubscription<T> subscription) {
        subscriptions.activate(subscription);
        unblock();
    }

    @Override
    public synchronized StreamSubscription<T> next() {
        return subscriptions.next();
    }
}
