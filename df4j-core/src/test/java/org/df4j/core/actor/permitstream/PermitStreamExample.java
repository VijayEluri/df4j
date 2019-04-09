package org.df4j.core.actor.permitstream;

import org.df4j.core.actor.StreamOutput;
import org.df4j.core.asyncproc.AllOf;
import org.df4j.core.asyncproc.Semafor;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ext.Actor1;
import org.df4j.core.actor.ext.StreamProcessor;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *  This is a demonstration how backpressure can be implemented using plain {@link Semafor}
 */
public class PermitStreamExample {

    /** making a feedback loop: permits flow from {@link Sink} to {@link Source#backPressureActuator}.
     */
    @Test
    public void piplineTest() throws InterruptedException, TimeoutException, ExecutionException {
        int totalCount = 10;
        Source first = new Source(totalCount);
        TestProcessor testProcessor = new TestProcessor();
        TestProcessor testProcessor1 = new TestProcessor();
        Sink last = new Sink(first.backPressureActuator);

        first.pub.subscribe(testProcessor);
        testProcessor.subscribe(testProcessor1);
        testProcessor1.subscribe(last);
        first.start();

        AllOf all = new AllOf();
        all.registerAsyncResult(first, testProcessor, testProcessor1, last);
        last.asyncResult().get(400, TimeUnit.MILLISECONDS);
        assertEquals(totalCount, last.totalCount);
    }

    public static class Source extends Actor {
        Semafor backPressureActuator = new Semafor(this);
        StreamOutput<Integer> pub = new StreamOutput<>(this);
        int count;

        Source(int count) {
            this.count = count;
        }

        protected void runAction() {
            if (count > 0) {
                pub.onNext(count);
                count--;
            } else {
                pub.onComplete();
            }
        }
    }

    static class TestProcessor extends StreamProcessor<Integer, Integer> {
        {
            start();
        }

        @Override
        protected Integer process(Integer message) {
            return message;
        }
    }

    static class Sink extends Actor1<Integer> {
        final Semafor backPressureActuator;
        int totalCount = 0;

        Sink(Semafor backPressureActuator) {
            this.backPressureActuator = backPressureActuator;
            backPressureActuator.release();
            start();
        }

        protected void runAction(Integer message) throws Exception {
            totalCount++;
            backPressureActuator.release();
        }

        @Override
        protected void completion() throws Exception {
            asyncResult().onComplete();
        }
    }
}
