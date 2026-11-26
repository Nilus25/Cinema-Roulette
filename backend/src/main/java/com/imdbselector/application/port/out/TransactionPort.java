package com.imdbselector.application.port.out;

import java.util.function.Supplier;

/**
 * Outbound port defining an explicit transaction boundary for multi-step UseCases
 * without coupling the Application layer to SQL, JDBC, or framework proxies.
 */
@FunctionalInterface
public interface TransactionPort {
    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
