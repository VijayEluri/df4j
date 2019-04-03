package org.df4j.core.reactivestream;

import org.df4j.core.scalar.AllOf;
import org.reactivestreams.Subscriber;

import static org.df4j.core.reactivestream.ReactiveStreamExampleBase.println;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastSource extends Source<Long> {
    public ReactiveUnicastOutput<Long> pub = new ReactiveUnicastOutput<>(this);
    long val = 0;

    public UnicastSource(AllOf parent, int totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastSource(long totalNumber) {
        this.val = totalNumber;
    }

    public UnicastSource() {
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        pub.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            println("Source.pub.post("+val+")");
            pub.onNext(val);
            val--;
        } else {
            pub.onComplete();
            println("Source.pub.complete()");
            stop();
        }
    }
}
