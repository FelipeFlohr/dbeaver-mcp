package dev.felipeflohr.dbeavermcp.module.connection.factory.config;

import com.zaxxer.hikari.HikariConfig;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver.FirebirdHikariDriverConfig;
import dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver.OracleHikariDriverConfig;
import dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver.PostgresHikariDriverConfig;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@NullMarked
@Component
class HikariConfigFactoryImpl implements HikariConfigFactory {
    @Override
    public HikariConfig getHikariConfig(String provider, String jdbcUrl, String username, @Nullable String password) throws DBeaverMCPValidationException {
        final String postgresProvider = DatabaseType.POSTGRES.getProvider();
        final String oracleProvider = DatabaseType.ORACLE.getProvider();
        final String firebirdProvider = DatabaseType.FIREBIRD.getProvider();

        if (provider.equals(postgresProvider)) {
            return new PostgresHikariDriverConfig().getConfig(jdbcUrl, username, password);
        }
        if (provider.equals(oracleProvider)) {
            return new OracleHikariDriverConfig().getConfig(jdbcUrl, username, password);
        }
        if (provider.equals(firebirdProvider)) {
            return new FirebirdHikariDriverConfig().getConfig(jdbcUrl, username, password);
        }
        throw new DBeaverMCPValidationException("Unsupported provider \"%s\".".formatted(provider));
    }
}
