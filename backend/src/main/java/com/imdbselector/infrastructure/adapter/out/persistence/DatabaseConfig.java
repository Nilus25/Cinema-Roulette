package com.imdbselector.infrastructure.adapter.out.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Configures HikariCP, runs Flyway migrations, and provides a jOOQ DSLContext.
 */
public final class DatabaseConfig {

    private DatabaseConfig() {
    }

    public static HikariDataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    public static void migrate(HikariDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    public static DSLContext createDsl(HikariDataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
