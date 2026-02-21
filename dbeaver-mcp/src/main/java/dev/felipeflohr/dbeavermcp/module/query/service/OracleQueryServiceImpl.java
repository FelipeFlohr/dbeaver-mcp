package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oracle.sql.*;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

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

@Slf4j
@NullMarked
@Service
class OracleQueryServiceImpl extends GenericQueryServiceImpl {
    public OracleQueryServiceImpl(ConnectionManager poolManager) {
        super(poolManager);
    }

    @Override
    public List<StatementResponseDTO> executeReadOnlyStatements(List<String> statements, String connectionName) throws DBeaverMCPValidationException {
        if (statements.isEmpty()) return List.of();
        DataSource ds = poolManager.getDataSourceFromConnectionName(connectionName);

        try (Connection conn = ds.getConnection()) {
            try {
                Statement statement = conn.createStatement();
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
    private void parseRows(List<Map<String, Object>> rows, Connection connection) {
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();

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
