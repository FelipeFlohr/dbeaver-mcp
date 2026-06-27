package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthConnectionData;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public interface DBeaverCipherService {
    Map<String, DBeaverAuthConnectionData> getConnectionsAuthentication() throws DBeaverMCPValidationException;
}
