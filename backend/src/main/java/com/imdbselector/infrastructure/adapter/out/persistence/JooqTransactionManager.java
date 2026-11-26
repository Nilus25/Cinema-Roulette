package com.imdbselector.infrastructure.adapter.out.persistence;

import com.imdbselector.application.port.out.TransactionPort;
import org.jooq.ConnectionProvider;
import org.jooq.impl.DataSourceConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * ThreadLocal-bound JDBC/jOOQ transaction manager so repositories sharing the same
 * DSLContext automatically participate in the active transaction.
 */
public final class JooqTransactionManager implements TransactionPort {
    private final DataSource dataSource;
    private final ConnectionProvider connectionProvider;
    private final ThreadLocal<Connection> activeConnection = new ThreadLocal<>();

    public JooqTransactionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
        DataSourceConnectionProvider delegate = new DataSourceConnectionProvider(dataSource);
        this.connectionProvider = new ConnectionProvider() {
            @Override
            public Connection acquire() {
                Connection txConn = activeConnection.get();
                return txConn != null ? txConn : delegate.acquire();
            }

            @Override
            public void release(Connection connection) {
                if (activeConnection.get() != connection) {
                    delegate.release(connection);
                }
            }
        };
    }

    public ConnectionProvider connectionProvider() {
        return connectionProvider;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (activeConnection.get() != null) {
            return work.get();
        }
        try (Connection conn = dataSource.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            activeConnection.set(conn);
            try {
                T result = work.get();
                conn.commit();
                return result;
            } catch (Throwable ex) {
                try {
                    conn.rollback();
                } catch (SQLException suppressed) {
                    ex.addSuppressed(suppressed);
                }
                throw ex;
            } finally {
                activeConnection.remove();
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute database transaction", e);
        }
    }
}
