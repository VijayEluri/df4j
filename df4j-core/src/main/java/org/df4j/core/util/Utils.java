package org.df4j.core.util;

public class Utils {

    @SuppressWarnings("unchecked")
    public static <T extends Exception, R> R sneakyThrow(Exception t) throws T {
        throw (T) t;
    }

}
