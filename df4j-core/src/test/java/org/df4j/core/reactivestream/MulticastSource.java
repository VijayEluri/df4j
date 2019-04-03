package org.df4j.core.reactivestream;

import org.df4j.core.actor.ReactiveMulticastOutput;
import org.df4j.core.asynchproc.AllOf;
import org.df4j.core.asynchproc.ext.Action;
import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Integers and closes the stream
 */
public class MulticastSource extends Source<Long> {
    protected ReactiveMulticastOutput<Long> pub = new ReactiveMulticastOutput<>(this);
    long val = 0;

    public MulticastSource() {
    }

    public MulticastSource(long totalNumber) {
        this.val = totalNumber;
    }

    public MulticastSource(AllOf parent, long totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        pub.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val == 0) {
            ReactiveStreamMulticastTest.println("MulticastSource.pub.complete()");
            pub.onComplete();
            stop();
        } else {
            ReactiveStreamMulticastTest.println("MulticastSource.pub.post "+ val);
            pub.onNext(val);
            val--;
        }
    }
}
