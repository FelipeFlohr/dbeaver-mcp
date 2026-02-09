package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDataDTO;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public interface DBeaverCipherService {
    Map<String, DBeaverAuthConnectionDataDTO> getConnectionsAuthentication() throws DBeaverMCPValidationException;
}
