package dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver;

import com.zaxxer.hikari.HikariConfig;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PostgresHikariDriverConfig extends DefaultHikariDriverConfig {
    public PostgresHikariDriverConfig() {
        super(DatabaseType.POSTGRES);
    }

    @Override
    public HikariConfig getConfig(String jdbcUrl, String username, @Nullable String password) {
        HikariConfig config = super.getConfig(jdbcUrl, username, password);
        config.addDataSourceProperty("readOnlyContent", "true");
        return config;
    }
}
