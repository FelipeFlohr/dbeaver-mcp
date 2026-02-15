package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Slf4j
@NullMarked
@RequiredArgsConstructor
abstract class GenericQueryServiceImpl implements QueryService {
    protected final ConnectionManager poolManager;

    @Override
    public List<StatementResponseDTO> executeReadOnlyStatements(List<String> statements, String connectionName) throws DBeaverMCPValidationException {
        if (statements.isEmpty()) return List.of();

        DataSource ds = poolManager.getDataSourceFromConnectionName(connectionName);
        try (Connection conn = ds.getConnection()) {
            try {
                Statement statement = conn.createStatement();
                return getStatementResponses(statements, statement);
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            throw new DBeaverMCPValidationException("Not possible to execute query: " + e.getMessage(), e);
        }
    }

    protected StatementResponseDTO resultSetToStatementResponse(String sql, ResultSet rs) throws DBeaverMCPValidationException {
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
            return new StatementResponseDTO(sql, rows);
        } catch (SQLException e) {
            throw new DBeaverMCPValidationException("Cannot convert result set to proper response.", e);
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    protected List<StatementResponseDTO> getStatementResponses(List<String> sqls, Statement statement) throws SQLException, DBeaverMCPValidationException {
        List<StatementResponseDTO> responses = new ArrayList<>();
        for (String sql : sqls) {
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                ResultSet rs = statement.getResultSet();
                responses.add(resultSetToStatementResponse(sql, rs));
            }
            log.info("Statement {} executed successfully. Has result set: {}", sql, hasResultSet);
        }
        log.info("Finished executing {} SQLs", sqls.size());
        return responses;
    }
}
