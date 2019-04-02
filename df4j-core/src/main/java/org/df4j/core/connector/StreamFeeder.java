package org.df4j.core.connector;

import org.df4j.core.Feeder;
import org.df4j.core.Port;
import org.df4j.core.node.AsyncProc;

import java.util.ArrayDeque;
import java.util.LinkedList;

/**
 * each input token it transferred to a single subscriber
 *
 * @param <T> type of tokens
 */
public class StreamFeeder<T> extends AsyncProc.Lock implements Port<T>, Feeder<T> {
    public static final int defaultCapacity = 16;

    protected final AsyncProc actor;
    protected final int capacity;
    protected final ArrayDeque<T> messages;
    protected LinkedList<Port<? super T>> subscriptions = new LinkedList<>();
    protected boolean done = false;
    protected Throwable completionToken;

    public StreamFeeder(AsyncProc actor, int capacity) {
        actor.super(false);
        this.actor = actor;
        this.capacity = capacity;
        messages = new ArrayDeque<>(capacity);
    }

    public StreamFeeder(AsyncProc actor) {
        this(actor, defaultCapacity);
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public void subscribe(Port<? super T> subscriber) {
        Throwable completionToken = null;
        T message;
        synchronized (this) {
            completionToken = this.completionToken;
            message = messages.pollFirst();
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
            purge();
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
            } else if (messages.size() == capacity) {
                throw new IllegalStateException();
            } else {
                messages.addLast(item);
                if (messages.size() == capacity) {
                    super.block();
                }
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
        return messages.pollFirst();
    }

    public synchronized T poll() {
        return messages.pollFirst();
    }
}
