package org.df4j.core.connector;

import org.df4j.core.node.AsyncProc;

/**
 * Token storage with standard Port&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends AsyncProc.BaseInput<T> {
    protected T current = null;
    protected Throwable exception;

    protected boolean pushback = false; // if true, do not consume

    public ScalarInput(AsyncProc task) {
        task.super();
    }

    public synchronized T current() {
        if (exception != null) {
            throw new IllegalStateException(exception);
        }
        return current;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isDone() {
        return current != null || exception != null;
    }

    @Override
    public void onNext(T message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (isDone()) {
            throw new IllegalStateException("token set already");
        }
        current = message;
        unblock();
    }

    @Override
    public void onError(Throwable throwable) {
        if (isDone()) {
            throw new IllegalStateException("token set already");
        }
        this.exception = throwable;
    }

    @Override
    public void onComplete() {
        if (isDone()) {
            return;
        }
        unblock();
    }

    @Override
    public void purge() {
        super.purge();
        current = null;
    }
// ===================== backend

    public boolean hasNext() {
        return !isDone();
    }

    public T next() {
        if (exception != null) {
            throw new RuntimeException(exception);
        }
        if (current == null) {
            throw new IllegalStateException();
        }
        T res = current;
        if (pushback) {
            pushback = false;
            // value remains the same, the pin remains turned on
        } else {
            current = null;
            block();
        }
        return res;
    }
}
