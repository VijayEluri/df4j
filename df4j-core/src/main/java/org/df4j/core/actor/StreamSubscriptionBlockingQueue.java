package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.HashSet;

/**
 * unblocks when there are active subscribers
 */
public class StreamSubscriptionBlockingQueue<T> extends Transition.Pin
        implements SubscriptionListener<T, StreamSubscription<T>>, Publisher<T> {
    protected ScalarSubscriptionQueue<T> activeSubscriptions = new ScalarSubscriptionQueue<>();
    protected HashSet<StreamSubscription<T>> passiveSubscriptions = new HashSet<>();
    protected boolean completed = false;
    protected volatile Throwable completionException;

    public StreamSubscriptionBlockingQueue(AsyncProc actor) {
        actor.super();
    }

    protected boolean isParameter() {
        return true;
    }

    private void complete(Subscriber<? super T> s) {
        if (completionException == null) {
            s.onComplete();
        } else {
            s.onError(completionException);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription;
        synchronized(this) {
            if (completed) {
                subscription = null;
            } else {
                subscription = new StreamSubscription(this, s);
                passiveSubscriptions.add(subscription);
            }
        }
        if (subscription == null) {
            complete(s);
        } else {
            s.onSubscribe(subscription);
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscriptionQueue<T> activeSubscriptions;
        HashSet<StreamSubscription<T>> passiveSubscriptions;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            activeSubscriptions = this.activeSubscriptions;
            this.activeSubscriptions = null;
            passiveSubscriptions = this.passiveSubscriptions;
            this.passiveSubscriptions = null;
        }
        for (StreamSubscription<T> subs: passiveSubscriptions) {
            subs.onError(ex);
        }
        for (ScalarSubscription subs: activeSubscriptions) {
            subs.onError(ex);
        }
    }

    public void onComplete() {
        ScalarSubscriptionQueue<T> activeSubscriptions;
        HashSet<StreamSubscription<T>> passiveSubscriptions;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            activeSubscriptions = this.activeSubscriptions;
            this.activeSubscriptions = null;
            passiveSubscriptions = this.passiveSubscriptions;
            this.passiveSubscriptions = null;
        }
        for (StreamSubscription<T> subs: passiveSubscriptions) {
            subs.onComplete();
        }
        for (ScalarSubscription subs: activeSubscriptions) {
            ((StreamSubscription)subs).onComplete();
        }
    }

    public synchronized StreamSubscription<T> current() {
        return (StreamSubscription) activeSubscriptions.peek();
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if suscription was removed
     *         false if subscription not found
     */
    public synchronized boolean remove(StreamSubscription<T> subscription) {
        if (subscription.getRequested() == 0) {
            return passiveSubscriptions.remove(subscription);
        } else {
            boolean res = activeSubscriptions.remove(subscription);
            if (isFull()) {
                block();
            }
            return res;
        }
    }

    protected boolean isFull() {
        return activeSubscriptions.isEmpty();
    }


    @Override
    public void serveRequest(StreamSubscription<T> subscription) {
        if (completed) {
            return;
        }
        passiveSubscriptions.remove(subscription);
        activeSubscriptions.add(subscription);
        unblock();
    }

    @Override
    public synchronized void purge() {
        StreamSubscription<T> current = (StreamSubscription<T>) activeSubscriptions.poll();
        if (current.getRequested() == 0) {
            passiveSubscriptions.add(current);
        } else {
            activeSubscriptions.add(current);
        }
    }
}
