package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReactiveSubscriberPromiseTest {

    @Test
    public void singleSubscriberTest() throws InterruptedException, ExecutionException, TimeoutException {
        AsyncResult<Double> sp0 = new AsyncResult<>();
        AsyncResult<Double> sp1 = new AsyncResult<>();
        AsyncResult<Double> sp2 = new AsyncResult<>();
        sp0.subscribe(sp1);
        sp0.subscribe(sp2);
        double v = 4.0;
        sp0.complete(v);
        double val1 = sp1.get(1, TimeUnit.SECONDS).doubleValue();
        Assert.assertEquals(v, val1, 0.0001);
        double val2 = sp2.get(1, TimeUnit.SECONDS).doubleValue();
        Assert.assertEquals(v, val2, 0.0001);
    }
}
