package org.df4j.core.connectornode;

import org.df4j.core.Feeder;
import org.df4j.core.Port;
import org.df4j.core.connector.ScalarFeeder;
import org.df4j.core.connector.ScalarInput;
import org.df4j.core.connector.StreamFeeder;
import org.df4j.core.node.Actor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  An asynchronous analogue of BlockingQueue
 *  (only on output end, while from the input side it does not block)
 *
 * @param <T> the type of the values passed through this token container
 */
public class SubmissionFeeder<T> extends Actor implements Feeder<T> {
    ScalarInput<T> input = new ScalarInput<>(this);
    ScalarFeeder<T> output = new ScalarFeeder<>(this);

    {
        setExecutor(directExec);
        start();
    }

    public synchronized boolean offer(T token) {
        if (!input.isBlocked()) {
            return false;
        }
        input.onNext(token);
        return true;
    }

    public synchronized void put(T token) throws InterruptedException {
        while (!input.isBlocked()) {
            wait();
        }
        input.onNext(token);
    }

    @Override
    public void subscribe(Port<? super T> subscriber) {
        output.subscribe(subscriber);
    }

    public T take() throws InterruptedException {
        CompletablePromise<T> future;
        synchronized (this) {
            T token = output.pollFirst();//
            if (token != null) {
                return token; // TODO filure
            }
            future = new CompletablePromise<>();
            output.subscribe(future);
        }
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        CompletablePromise<T> future;
        synchronized (this) {
            T token = output.pollFirst();//
            if (token != null) {
                return token; // TODO filure
            }
            future = new CompletablePromise<>();
            output.subscribe(future);
        }
        try {
            return future.get(timeout, unit);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void runAction() throws Throwable {
        output.onNext(input.current());
    }
}
