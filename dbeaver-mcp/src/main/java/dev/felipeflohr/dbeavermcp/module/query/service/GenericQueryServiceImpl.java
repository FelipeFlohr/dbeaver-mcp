package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import dev.felipeflohr.dbeavermcp.module.entityparser.mcp.EntityParserMCPService;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementErrorResponseDTO;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@NullMarked
@RequiredArgsConstructor
abstract class GenericQueryServiceImpl implements QueryService {
    protected final ConnectionManager poolManager;
    private final ObjectMapper objectMapper;

    @Override
    public List<StatementResponseDTO> executeReadOnlyStatements(List<String> statements, String connectionName, @Nullable Integer timeoutInSeconds) throws DBeaverMCPValidationException {
        if (statements.isEmpty()) return List.of();

        int timeout = Optional.ofNullable(timeoutInSeconds).orElse(30);
        DataSource ds = poolManager.getDataSourceFromConnectionName(connectionName);
        try (Connection conn = ds.getConnection()) {
            try {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(timeout);
                return getStatementResponses(statements, statement);
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            throw new DBeaverMCPValidationException("Not possible to execute query: " + e.getMessage(), e);
        }
    }

    @Override
    public File executeReadOnlyStatementsAsync(List<String> statements, String connectionName, @Nullable Integer timeoutInSeconds) throws DBeaverMCPValidationException {
        try {
            String tmpFilePrefix = "result-%s-%s".formatted(connectionName, LocalDateTime.now());
            File tmpFile = File.createTempFile(tmpFilePrefix, ".json");

            Thread.ofVirtual().start(() -> {
                try (var writer = new FileWriter(tmpFile)) {
                    try {
                        List<StatementResponseDTO> responses = executeReadOnlyStatements(statements, connectionName, timeoutInSeconds);
                        String resultAsJson = objectMapper.writeValueAsString(responses);
                        writer.write(resultAsJson);
                        log.info("Successfully executed {} statements asynchronously", responses.size());
                    } catch (DBeaverMCPValidationException e) {
                        String msg = "Failed to execute async statements." +
                                "\nStatements:" + statements +
                                "\nStack trace: " + StringUtils.getExceptionStackTraceAsString(e);
                        writer.write(msg);
                        log.error("Failed to execute async statements", e);
                    }
                } catch (IOException e) {
                    log.error("Failed to write to file {}.", tmpFile.getAbsolutePath(), e);
                }
            });
            return tmpFile;
        } catch (IOException e) {
            throw new DBeaverMCPValidationException("Failed to create async statement file: " + e.getMessage(), e);
        }
    }

    protected StatementResponseDTO resultSetToStatementResponse(String sql, ResultSet rs) {
        long startMillis = System.currentTimeMillis();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }

            long finishMillis = System.currentTimeMillis();
            return new StatementResponseDTO(sql, rows, null, finishMillis - startMillis);
        } catch (SQLException e) {
            return createResponseFromError(sql, e, startMillis);
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    protected List<StatementResponseDTO> getStatementResponses(List<String> sqls, Statement statement) throws SQLException {
        List<StatementResponseDTO> responses = new ArrayList<>();
        for (String sql : sqls) {
            long startMillis = System.currentTimeMillis();
            try {
                boolean hasResultSet = statement.execute(sql);
                if (hasResultSet) {
                    ResultSet rs = statement.getResultSet();
                    responses.add(resultSetToStatementResponse(sql, rs));
                }
                log.info("Statement {} executed successfully. Has result set: {}", sql, hasResultSet);
            } catch (SQLException e) {
                responses.add(createResponseFromError(sql, e, startMillis));
            }
        }
        log.info("Finished executing {} SQLs", sqls.size());
        return responses;
    }

    protected abstract boolean errorIsAboutInvalidIdentifier(SQLException ex);

    private StatementResponseDTO createResponseFromError(String sql, SQLException e, long startMillis) {
        long finishMillis = System.currentTimeMillis();
        log.error("Cannot convert result set to proper response", e);

        String hint = null;
        if (errorIsAboutInvalidIdentifier(e)) {
            hint = "You have an invalid identifier in your SQL. To avoid expend unnecessary tokens and time, try " +
                    "using the \"%s\" MCP tool.".formatted(EntityParserMCPService.GET_TABLE_COLUMNS_FROM_JPA_ENTITY_TOOL_NAME);
        }
        var errorStatement = new StatementErrorResponseDTO(e.getMessage(), StringUtils.getExceptionStackTraceAsString(e), hint);
        return new StatementResponseDTO(sql, null, errorStatement, finishMillis - startMillis);
    }
}
