package org.df4j.core.boundconnector.messagescalar;

import org.df4j.core.tasknode.AsyncProc;

import java.util.function.BiConsumer;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface. It has place for only one
 * token, which is never consumed.
 *
 * @param <T>
 *     type of accepted tokens.
 */
public class ConstInput<T> extends AsyncProc.AsyncParam<T>
        implements ScalarSubscriber<T>  // to connect to a ScalarPublisher
{
    protected SimpleSubscription subscription;
    protected boolean closeRequested = false;
    protected boolean cancelled = false;

    /** extracted token */
    protected boolean completed = false;
    protected T value = null;
    protected Throwable exception;

    public ConstInput(AsyncProc task) {
        task.super();
    }

    @Override
    public synchronized void onSubscribe(SimpleSubscription subscription) {
        if (closeRequested) {
            subscription.cancel();
        } else {
            this.subscription = subscription;
        }
    }

    public synchronized T current() {
        if (exception != null) {
            throw new IllegalStateException(exception);
        }
        return value;
    }

    public T getValue() {
        return value;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isDone() {
        return completed || exception != null;
    }

    /**
     * pin bit remains ready
     */
    @Override
    public T next() {
        return current();
    }

    @Override
    public boolean complete(T message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (isDone()) {
            throw new IllegalStateException("token set already");
        }
        value = message;
        turnOn();
        return true;
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        if (isDone()) {
            throw new IllegalStateException("token set already");
        }
        this.exception = throwable;
        return true;
    }

    public synchronized boolean cancel() {
        if (subscription == null) {
            return cancelled;
        }
        SimpleSubscription subscription = this.subscription;
        this.subscription = null;
        cancelled = true;
        boolean result = subscription.cancel();
        return result;
    }
}
