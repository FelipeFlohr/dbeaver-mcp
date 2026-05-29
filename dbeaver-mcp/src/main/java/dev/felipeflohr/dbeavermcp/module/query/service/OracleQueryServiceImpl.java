package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oracle.sql.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@NullMarked
@Service
class OracleQueryServiceImpl extends GenericQueryServiceImpl {
    private static final String TABLE_OR_COLUMN_UNKNOWN_ERROR = "42000";

    public OracleQueryServiceImpl(ConnectionManager poolManager, ObjectMapper objectMapper) {
        super(poolManager, objectMapper);
    }

    @Override
    public List<StatementResponseDTO> executeReadOnlyStatements(List<String> statements, String connectionName, @Nullable Integer timeoutInSeconds) throws DBeaverMCPValidationException {
        if (statements.isEmpty()) return List.of();
        DataSource ds = poolManager.getDataSourceFromConnectionName(connectionName);

        int timeout = Optional.ofNullable(timeoutInSeconds).orElse(30);
        try (Connection conn = ds.getConnection()) {
            try {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(timeout);
                statement.execute("SET TRANSACTION READ ONLY");
                List<StatementResponseDTO> responses = getStatementResponses(statements, statement);
                responses.forEach(r -> parseRows(r.getResponse(), conn));
                return responses;
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            throw new DBeaverMCPValidationException("Not possible to execute query: " + e.getMessage(), e);
        }
    }

    @SneakyThrows
    private void parseRows(@Nullable List<Map<String, @Nullable Object>> rows, Connection connection) {
        if (rows == null) return;
        for (Map<String, @Nullable Object> row : rows) {
            for (Map.Entry<String, @Nullable Object> entry : row.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                switch (value) {
                    case CLOB oracleClob -> row.put(columnName, oracleClob.stringValue());
                    case Timestamp timestamp -> row.put(columnName, timestamp.toLocalDateTime());
                    case TIMESTAMP oracleTimestamp -> row.put(columnName, oracleTimestamp.toLocalDateTime());
                    case TIMESTAMPTZ oracleTimestampTZ -> row.put(columnName, oracleTimestampTZ.toLocalDateTime());
                    case TIMESTAMPLTZ oracleTimestampLTZ -> row.put(columnName, oracleTimestampLTZ.toLocalDateTime(connection));
                    case INTERVALYM oracleIntervalYM -> row.put(columnName, oracleIntervalYM.stringValue());
                    case INTERVALDS oracleIntervalDS -> row.put(columnName, oracleIntervalDS.stringValue());
                    case BLOB oracleBlob -> row.put(columnName, "MCP converted this value to Base64 from a BLOB type: " + blobToBase64(oracleBlob));
                    default -> {}
                }
            }
        }
    }

    @Override
    protected boolean errorIsAboutInvalidIdentifier(SQLException ex) {
        final String errorCode = ex.getSQLState();
        return errorCode.toLowerCase().contains(TABLE_OR_COLUMN_UNKNOWN_ERROR.toLowerCase());
    }

    private String blobToBase64(BLOB blob) throws SQLException, IOException {
        try (
                InputStream is = blob.getBinaryStream();
                var baos = new ByteArrayOutputStream();
                OutputStream b64os = Base64.getEncoder().wrap(baos)
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                b64os.write(buffer, 0, bytesRead);
            }
            baos.close();
            return baos.toString(StandardCharsets.UTF_8);
        }
    }
}
