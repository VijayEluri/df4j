package org.df4j.core.tasknode.messagestream;

import org.df4j.core.boundconnector.messagestream.StreamOutput;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements Publisher<R> {
	protected final StreamOutput<R> output = new StreamOutput<>(this);

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction(M message) {
        R res = process(message);
        output.post(res);
    }

    @Override
    protected void completion() throws Exception {
        output.complete();
    }

    protected abstract R process(M message);

}
