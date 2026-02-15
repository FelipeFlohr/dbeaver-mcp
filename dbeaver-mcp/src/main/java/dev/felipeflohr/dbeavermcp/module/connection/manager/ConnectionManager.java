package dev.felipeflohr.dbeavermcp.module.connection.manager;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import org.jspecify.annotations.NullMarked;

import javax.sql.DataSource;

@NullMarked
public interface ConnectionManager {
    DataSource getDataSourceFromConnectionName(String connectionName) throws DBeaverMCPValidationException;
    DatabaseType getDatabaseTypeFromConnectionName(String connectionName) throws DBeaverMCPValidationException;
}
