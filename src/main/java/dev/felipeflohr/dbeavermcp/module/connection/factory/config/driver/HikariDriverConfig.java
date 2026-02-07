package dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver;

import com.zaxxer.hikari.HikariConfig;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Abstraction for Hikari's configuration.
 */
@NullMarked
public interface HikariDriverConfig {
    /**
     * Returns the config for the implemented driver.
     * @return {@link HikariConfig} for the driver.
     */
    HikariConfig getConfig(String jdbcUrl, String username, @Nullable String password);
}
