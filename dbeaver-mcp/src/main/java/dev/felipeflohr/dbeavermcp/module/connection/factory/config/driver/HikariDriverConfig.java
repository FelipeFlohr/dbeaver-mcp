package dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver;

import com.zaxxer.hikari.HikariConfig;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface HikariDriverConfig {
    HikariConfig getConfig(String jdbcUrl, String username, @Nullable String password);
}
