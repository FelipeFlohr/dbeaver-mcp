package dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver;

import com.zaxxer.hikari.HikariConfig;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@RequiredArgsConstructor
abstract class DefaultHikariDriverConfig implements HikariDriverConfig {
    private static final int FIVE_MINUTES_IN_MS = 300000;
    private static final int TWENTY_SECONDS_IN_MS = 20000;

    private final DatabaseType databaseType;

    @Override
    public HikariConfig getConfig(String jdbcUrl, String username, @Nullable String password) {
        var config = new HikariConfig();
        config.setDriverClassName(databaseType.getDriverClassName());
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setIdleTimeout(FIVE_MINUTES_IN_MS);
        config.setConnectionTimeout(TWENTY_SECONDS_IN_MS);
        config.setPoolName("Pool-%s-%s".formatted(databaseType.getProvider(), System.currentTimeMillis()));

        config.setReadOnly(true);
        config.setAutoCommit(false);
        return config;
    }
}
