package dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver;

import com.zaxxer.hikari.HikariConfig;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FirebirdHikariDriverConfig extends DefaultHikariDriverConfig {
    public FirebirdHikariDriverConfig() {
        super(DatabaseType.FIREBIRD);
    }

    @Override
    public HikariConfig getConfig(String jdbcUrl, String username, @Nullable String password) {
        HikariConfig config = super.getConfig(jdbcUrl, username, password);
        config.addDataSourceProperty("charSet", "UTF-8");
        return config;
    }
}
