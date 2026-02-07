package dev.felipeflohr.dbeavermcp.module.connection.factory.config.driver;

import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OracleHikariDriverConfig extends DefaultHikariDriverConfig {
    public OracleHikariDriverConfig() {
        super(DatabaseType.ORACLE);
    }
}
