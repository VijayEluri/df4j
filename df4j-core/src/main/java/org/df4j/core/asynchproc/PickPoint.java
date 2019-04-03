package org.df4j.core.asynchproc;

import org.df4j.core.Port;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

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
public class PickPoint<T> implements Publisher<T>, Port<T> {
    protected ArrayDeque<T> resources = new ArrayDeque<>();
    protected boolean completed = false;
	/** place for demands */
	private Queue<org.reactivestreams.Subscriber> requests = new ArrayDeque<>();

    public synchronized boolean isCompleted() {
        return completed;
    }

    @Override
	public synchronized void onNext(T token) {
        if (completed) {
            throw new IllegalStateException();
        }
	    if (requests.isEmpty()) {
            resources.add(token);
        } else {
	        requests.poll().onNext(token);
        }
	}

	@Override
	public synchronized void onComplete() {
        if (completed) {
            return;
        }
        completed = true;
        for (org.reactivestreams.Subscriber subscriber: requests) {
            subscriber.onComplete();
        }
        requests = null;
	}

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (completed) {
            throw new IllegalStateException();
        }
		if (resources.isEmpty()) {
			requests.add(subscriber);
		} else {
			subscriber.onNext(resources.poll());
		}
	}

    public T take() throws InterruptedException {
        synchronized(this) {
            if (!resources.isEmpty() && requests.isEmpty()) {
                return resources.remove();
            }
        }
        CompletablePromise<T> future = new CompletablePromise<>();
        subscribe(future);
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized(this) {
            if (!resources.isEmpty() && requests.isEmpty()) {
                return resources.remove();
            }
        }
        CompletablePromise<T> future = new CompletablePromise<>();
        subscribe(future);
        try {
            return future.get(timeout, unit);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
