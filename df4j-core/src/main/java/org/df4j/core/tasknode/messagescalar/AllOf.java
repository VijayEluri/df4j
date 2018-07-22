package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.simplenode.messagescalar.SubscriberPromise;

public class AllOf extends AsyncSupplier<Object[]> {
    Object[] results;

    public AllOf(SubscriberPromise<?>... sources) {
        results = new Object[sources.length];
        for (int k = 0; k<sources.length; k++) {
            SubscriberPromise source = sources[k];
            final Enter arg = new Enter(k);
            source.subscribe(arg);
        }
    }

    @Override
    protected void fire() {
        complete(results);
    }

    class Enter extends Lock implements ScalarSubscriber<Object> {
        private final int num;

        public Enter(int num) {
            this.num = num;
        }

        @Override
        public void post(Object value) {
            results[num] = value;
            super.turnOn();
        }

        @Override
        public void postFailure(Throwable ex) {
            synchronized (AllOf.this) {
                if (!result.isDone()) {
                    AllOf.this.completeExceptionally(ex);
                }
            }
        }
    }

}
