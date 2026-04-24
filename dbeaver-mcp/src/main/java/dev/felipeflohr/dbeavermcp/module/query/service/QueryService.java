package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;

@NullMarked
public interface QueryService {
    List<StatementResponseDTO> executeReadOnlyStatements(List<String> statements, String connectionName, @Nullable Integer timeoutInSeconds) throws DBeaverMCPValidationException;
    File executeReadOnlyStatementsAsync(List<String> statements, String connectionName, @Nullable Integer timeoutInSeconds) throws DBeaverMCPValidationException;
}
