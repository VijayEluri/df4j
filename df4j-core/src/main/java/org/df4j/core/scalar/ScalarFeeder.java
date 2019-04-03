package org.df4j.core.scalar;

import org.df4j.core.Feeder;
import org.df4j.core.Port;
import org.df4j.core.scalar.AsyncProc;

import java.util.LinkedList;

/**
 * each input token it transferred to a single subscriber
 *
 * @param <T> type of tokens
 */
public class ScalarFeeder<T> extends AsyncProc.Lock implements Port<T>, Feeder<T> {
    public static final int defaultCapacity = 16;

    protected final AsyncProc actor;
    protected T message;
    protected LinkedList<Port<? super T>> subscriptions = new LinkedList<>();
    protected boolean done = false;
    protected Throwable completionToken;

    public ScalarFeeder(AsyncProc actor) {
        actor.super(false);
        this.actor = actor;
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public void subscribe(Port<? super T> subscriber) {
        Throwable completionToken = null;
        T message = null;
        synchronized (this) {
            completionToken = this.completionToken;
            message = this.message;
            if (message == null && !done) {
                subscriptions.addLast(subscriber);
                return;
            }
        }
        if (message != null) {
            subscriber.onNext(message);
            super.unblock();
        } else if (completionToken == null) {
            subscriber.onComplete();
        } else {
            subscriber.onError(completionToken);
        }
    }

    public synchronized void unSubscribe(Port<? super T> subscriber) {
        subscriptions.remove(subscriber);
    }

    public void onNext(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        Port<? super T> subscriber;
        synchronized (this) {
            if (done) {
                throw new IllegalStateException();
            }
            if (!subscriptions.isEmpty()) {
                subscriber = subscriptions.pollFirst();
                subscriptions.addLast(subscriber);
            } else if (message != null) {
                throw new IllegalStateException();
            } else {
                message = item;
                super.block();
                return;
            }
            subscriber.onNext(item);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LinkedList<Port<? super T>> subscriptions;
        synchronized (this) {
            if (done) {
                throw new IllegalStateException();
            }
            this.completionToken = throwable;
            this.done = true;
            subscriptions = this.subscriptions;
            this.subscriptions = null;
        }
        for (Port<? super T> subscription: subscriptions) {
            subscription.onError(throwable);
        }
    }

    public synchronized void onComplete() {
        LinkedList<Port<? super T>> subscriptions;
        synchronized (this) {
            if (done) {
                throw new IllegalStateException();
            }
            this.done = true;
            subscriptions = this.subscriptions;
            this.subscriptions = null;
        }
        for (Port<? super T> subscription: subscriptions) {
            subscription.onComplete();
        }
    }

    public synchronized T pollFirst() {
        return message;
    }

    public synchronized T poll() {
        return message;
    }
}
