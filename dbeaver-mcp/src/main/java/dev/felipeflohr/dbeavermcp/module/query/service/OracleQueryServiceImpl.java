package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
                return getStatementResponses(statements, statement);
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            throw new DBeaverMCPValidationException("Not possible to execute query: " + e.getMessage(), e);
        }
    }
}
