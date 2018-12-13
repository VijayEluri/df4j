package org.df4j.core.rxjava2;

import io.reactivex.functions.Consumer;
import org.df4j.core.tasknode.messagescalar.AsyncBiFunction;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * An example of an adapter class able to connect to Rxjava Observable
 *
 * @param <T> the type of the first parameter
 * @param <U> the type of the second parameter
 * @param <V> the type of the result
 */
public class AsyncBiFunctionRx<T,U,V> extends AsyncBiFunction<T,U,V> {

    public final Consumer<T> rxparam1 = v->param1.post(v);

    public final Consumer<U> rxparam2 = v->param2.post(v);

    public AsyncBiFunctionRx(BiFunction fn) {
        super(fn);
    }

    public AsyncBiFunctionRx(BiConsumer action) {
        super(action);
    }
}
