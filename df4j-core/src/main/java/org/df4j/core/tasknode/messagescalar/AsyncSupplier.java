package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.util.invoker.Invoker;
import org.df4j.core.util.invoker.RunnableInvoker;
import org.df4j.core.util.invoker.SupplierInvoker;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Base class for scalar nodes
 * Has predefined unbound output connector to keep the result of computation.
 *
 * Even if the computation does not produce a resulting value,
 * that connector is useful to monitor the end of the computation.
 *
 * @param <R> type of the result
 */
public class AsyncSupplier<R> extends AsyncAction<R> implements ScalarPublisher<R>, Future<R> {

    public AsyncSupplier() {}

    public AsyncSupplier(Invoker invoker) {
        super(invoker);
    }

    public AsyncSupplier(Supplier<R> proc) {
        super(new SupplierInvoker<>(proc));
    }

    public AsyncSupplier(Runnable proc) {
        super(new RunnableInvoker<R>(proc));
    }

    @Override
    public CompletablePromise<R> asyncResult() {
        return super.asyncResult();
    }

    @Override
    public Subscription subscribe(ScalarSubscriber<R> subscriber) {
        return asyncResult().subscribe(subscriber);
    }

    protected boolean completeResult(R res) {
        return asyncResult().complete(res);
    }

    protected boolean completeResultExceptionally(Throwable ex) {
        return result.completeExceptionally(ex);
    }

    @Override
    public boolean cancel(boolean b) {
        return result.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return result.isCancelled();
    }

    @Override
    public boolean isDone() {
        return result.isDone();
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        return asyncResult().get();
    }

    @Override
    public R get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return asyncResult().get(l, timeUnit);
    }

    @Override
    public void run() {
        try {
            blockStarted();
            R res = callAction();
            asyncResult().complete(res);
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
        stop();
    }
}
