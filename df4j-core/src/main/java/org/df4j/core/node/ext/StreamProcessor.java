package org.df4j.core.node.ext;


import org.df4j.core.Feeder;
import org.df4j.core.Port;
import org.df4j.core.connector.StreamFeeder;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements Feeder<R> {
	protected final StreamFeeder<R> output = new StreamFeeder<>(this);

    @Override
    public void subscribe(Port<? super R> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction(M message) {
        R res = process(message);
        output.onNext(res);
    }

    @Override
    protected void completion() throws Exception {
        output.onComplete();
    }

    protected abstract R process(M message);

}
