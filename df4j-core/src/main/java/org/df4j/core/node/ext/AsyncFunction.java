package org.df4j.core.node.ext;

import org.df4j.core.Port;
import org.df4j.core.connector.ScalarInput;
import org.df4j.core.util.invoker.ConsumerInvoker;
import org.df4j.core.util.invoker.FunctionInvoker;
import org.df4j.core.util.invoker.RunnableInvoker;

import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncFunction<T, R> extends AsyncSupplier<R> implements Port<T> {
    protected final ScalarInput<T> argument = new ScalarInput<>(this);

    public AsyncFunction() { }

    public AsyncFunction(Function<T,R> fn) {
        super(new FunctionInvoker<>(fn));
    }

    public AsyncFunction(Consumer<? super T> action) {
        super(new ConsumerInvoker<>(action));
    }

    public AsyncFunction(Runnable action) {
        super(new RunnableInvoker<>(action));
    }

    @Override
    public void subscribe(Port<? super R> subscriber) {
        super.asyncResult().subscribe(subscriber);
    }

    @Override
    public void onNext(T message) {
        argument.onNext(message);
    }

    @Override
    public void onError(Throwable throwable) {
        argument.onError(throwable);
    }

    @Override
    public void onComplete() {
        onNext(null);
    }
}
