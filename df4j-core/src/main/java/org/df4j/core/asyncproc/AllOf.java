package org.df4j.core.asyncproc;

import org.df4j.core.Port;
import org.df4j.core.asyncproc.ext.AsyncSupplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

public class AllOf extends AsyncSupplier<Void> {

    public AllOf() {
    }

    public AllOf(Publisher<?>... sources) {
        for (Publisher source: sources) {
            registerAsyncResult(source);
        }
    }

    /**
     * blocks this instance from completion until the source is completed.
     *
     * @param source source of completion. successfull or unseccessfull
     */
    public synchronized void registerAsyncResult(Publisher source) {
        source.subscribe(new Enter());
    }

    public synchronized void registerAsyncResult(AsyncProc... sources) {
        for (AsyncProc source: sources) {
            registerAsyncResult(source.asyncResult());
        }
    }

    /**
     * does not blocks this instance from completion.
     * Used to collect possible exceptions only
     *
     * @param source source of errors
     */
    public synchronized void registerAsyncDaemon(Publisher source) {
        source.subscribe(new DaemonEnter());
    }

    @Override
    protected void fire() {
        completeResult(null);
    }

    protected synchronized void postGlobalFailure(Throwable ex) {
        completeResultExceptionally(ex);
    }

    class Enter extends Pin implements Port<Object> {
        Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
        }

        @Override
        public void onNext(Object value) {
            super.unblock();
        }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }

    class DaemonEnter implements Port<Object> {

        @Override
        public void onNext(Object value) { }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }
}
