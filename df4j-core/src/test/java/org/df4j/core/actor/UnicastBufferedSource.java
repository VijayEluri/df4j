package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastBufferedSource extends Source<Long> {
    public StreamOutput<Long> output = new StreamOutput<>(this);
    long val = 0;

    public UnicastBufferedSource(Logger parent, int totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastBufferedSource(long totalNumber) {
        this.val = totalNumber;
        if (totalNumber == 0) {
            output.onComplete();
        }
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            println("UnicastBufferedSource:subscription.onNext("+val+")");
            output.onNext(val);
            val--;
        } else {
            println("UnicastBufferedSource:subscription.onComplete()");
            output.onComplete();
            stop();
        }
    }
}
