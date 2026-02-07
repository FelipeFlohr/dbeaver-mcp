package dev.felipeflohr.dbeavermcp.module.connection.manager;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import org.jspecify.annotations.NullMarked;

import javax.sql.DataSource;

/**
 * Manages the creation of database connections.
 */
@NullMarked
public interface ConnectionPoolManager {
    /**
     * Creates a {@link DataSource} from a connection name, searching the connection name on the DBeaver's connection
     * list.
     * @param connectionName The connection name on DBeaver.
     * @return {@link DataSource} for the specified connection name.
     * @throws DBeaverMCPValidationException if fails to found connection or authentication data.
     */
    DataSource getDataSourceFromConnectionName(String connectionName) throws DBeaverMCPValidationException;

    /**
     * Retrieves the database type from a connection name, searching the connection name on the DBeaver's connection
     * list.
     * @param connectionName The connection name on DBeaver.
     * @return {@link DatabaseType} for the specified connection name.
     * @throws DBeaverMCPValidationException if fails to found connection.
     */
    DatabaseType getDatabaseTypeFromConnectionName(String connectionName) throws DBeaverMCPValidationException;
}
