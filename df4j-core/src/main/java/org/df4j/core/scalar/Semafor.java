package org.df4j.core.scalar;

import org.df4j.core.PermitSubscriber;
import org.df4j.core.scalar.AsyncProc;

/**
 * Counting semaphore
 * holds token counter without data.
 * counter can be negative.
 */
public class Semafor extends AsyncProc.Lock implements PermitSubscriber {
    protected final AsyncProc actor;
    private long count = 0;

    public Semafor(AsyncProc actor, int count) {
        actor.super(count <= 0);
        this.actor = actor;
        this.count = count;
    }

    public Semafor(AsyncProc actor) {
        this(actor, 0);
    }

    public long getCount() {
        return count;
    }

    /** increments resource counter by delta */
    @Override
    public synchronized void release(long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("resource counter delta must be >= 0");
        }
        long prev = count;
        count+= delta;
        if (prev <= 0 && count > 0 ) {
            unblock();
        }
    }

    @Override
    public synchronized void purge() {
        long prev = count;
        count--;
        if (prev > 0 && count <= 0 ) {
            block();
        }
    }
}
