package org.df4j.core.node;

import org.df4j.core.connectornode.CompletablePromise;
import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.Invoker;

import java.util.concurrent.Executor;

/**
 * this class contains components, likely useful in each async task node:
 *  - control pin -- prevents concurrent execution of the same AsyncAction
 *  - action caller -- allows @Action annotation
 *  - scalar result. Even if this action will produce a stream of results or no result at all,
 *  it can be used as a channel for unexpected errors.
 */
public class AsyncAction<R> extends AsyncProc {
    private static final Object[] emptyArgs = new Object[0];

    /**
     * blocked initially, until {@link #start} called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    protected Lock controlLock = new Lock();

    protected Invoker actionCaller;

    protected final CompletablePromise<R> result = new CompletablePromise<>();

    /**
     * if true, this action cannot be restarted
     */
    protected volatile boolean stopped = false;

    private boolean argsPurged;

    public AsyncAction() {
    }

    public AsyncAction(Invoker actionCaller) {
        this.actionCaller = actionCaller;
    }

    public boolean isStarted() {
        return !controlLock.isBlocked();
    }

    public boolean isStopped() {
        return stopped;
    }

    public CompletablePromise<R> asyncResult() {
        return result;
    }

    public synchronized void start() {
        if (stopped) {
            throw new IllegalStateException();
        }
        argsPurged = false;
        controlLock.unblock();
    }

    public synchronized void start(Executor executor) {
        setExecutor(executor);
        start();
    }

    protected void blockStarted() {
        controlLock.block();
    }

    public synchronized void stop() {
        stopped = true;
        if (!result.isDone()) {
            result.onComplete();
        }
    }

    public String toString() {
        return super.toString() + result.toString();
    }

    protected synchronized Object[] extractArguments() {
        if (asyncParams == null) {
            return emptyArgs;
        } else {
            int paramCount = asyncParams.size();
            Object[] args = new Object[paramCount];
            for (int k = 0; k < paramCount; k++) {
                BaseInput<?> arg = asyncParams.get(k);
                args[k] = arg.current();
                arg.purge();
            }
            argsPurged = true;
            return args;
        }
    }

    protected synchronized void purgeAll() {
        if (locks != null) {
            for (int k = 0; k < locks.size(); k++) {
                locks.get(k).purge();
            }
        }
        if (!argsPurged) {
            if (asyncParams != null) {
                for (int k = 0; k < asyncParams.size(); k++) {
                    asyncParams.get(k).purge();
                }
            }
            argsPurged = true;
        }
    }

    protected R callAction() throws Throwable {
        if (actionCaller == null) {
            actionCaller = ActionCaller.findAction(this, getParamCount());
        }
        Object[] args = extractArguments();
        R  res = (R) actionCaller.apply(args);
        return res;
    }

    protected void runAction() throws Throwable {
        callAction();
    }

    @Override
    public void run() {
        try {
            blockStarted();
            runAction();
        } catch (Throwable e) {
            result.completeExceptionally(e);
            stop();
        }
    }
}
