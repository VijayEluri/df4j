package org.df4j.core;

public interface Feeder<T> {

    /**
     * automatically unsubscribed after the subscriber is given next token.
     * To get another token, one more call to this method is needed.
     *
     * @param subscriber
     */
    void subscribe(Port<? super T> subscriber);
}
