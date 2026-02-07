package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDataDTO;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Service for dealing with DBeaver's encrypted files, such as the connection file.
 */
@NullMarked
public interface DBeaverCipherService {
    /**
     * Retrieves the connections' authentication data.
     * @return Map with the connection name and authentication data.
     * @throws DBeaverMCPValidationException When an invalid parameter is passed.
     */
    Map<String, DBeaverAuthConnectionDataDTO> getConnectionsAuthentication() throws DBeaverMCPValidationException;
}
