package dev.felipeflohr.dbeavermcp.module.connection.factory.config;

import com.zaxxer.hikari.HikariConfig;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface HikariConfigFactory {
    HikariConfig getHikariConfig(String provider, String jdbcUrl, String username, @Nullable String password) throws DBeaverMCPValidationException;
}
