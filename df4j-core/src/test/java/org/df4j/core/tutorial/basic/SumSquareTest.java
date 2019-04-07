package org.df4j.core.tutorial.basic;

import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.AsyncResult;
import org.df4j.core.asyncproc.ScalarInput;
import org.df4j.core.asyncproc.ext.AsyncBiFunction;
import org.df4j.core.asyncproc.ext.AsyncFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SumSquareTest {

    public static class Square extends AsyncProc {
        final AsyncResult<Integer> result = new AsyncResult<>();
        final ScalarInput<Integer> param = new ScalarInput<>(this);

        public void run() {
            Integer arg = param.current();
            int res = arg*arg;
            result.complete(res);
        }
    }

    public static class Sum extends AsyncProc {
        final AsyncResult<Integer> result = new AsyncResult<>();
        final ScalarInput<Integer> paramX = new ScalarInput<>(this);
        final ScalarInput<Integer> paramY = new ScalarInput<>(this);

        public void run() {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            result.complete(res);
        }
    }

    @Test
    public void testAP() throws ExecutionException, InterruptedException, TimeoutException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.result.subscribe(sum.paramX);
        sqY.result.subscribe(sum.paramY);
        // provide input information:
        sqX.param.onNext(3);
        sqY.param.onNext(4);
        // get the result
        int res = sum.result.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

    @Test
    public void testDFF() throws ExecutionException, InterruptedException, TimeoutException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
        AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
        AsyncBiFunction<Integer, Integer, Integer> sum = new AsyncBiFunction<Integer, Integer, Integer>(plus);
        // make 2 connections
        sqX.subscribe(sum.param1);
        sqY.subscribe(sum.param2);
        // provide input information:
        sqX.onNext(3);
        sqY.onNext(4);
        // get the result
        int res = sum.asyncResult().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

    @Test
    public void testCF() throws ExecutionException, InterruptedException, TimeoutException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        CompletableFuture<Integer> sqXParam = new CompletableFuture();
        CompletableFuture<Integer> sqYParam = new CompletableFuture();
        CompletableFuture<Integer> sum = sqXParam
                .thenApply(square)
                .thenCombine(sqYParam.thenApply(square),
                        plus);
        // provide input information:
        sqXParam.complete(3);
        sqYParam.complete(4);
        // get the result
        int res = sum.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

}
