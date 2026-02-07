package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OracleQueryServiceTest extends BaseQueryServiceTest {
    @Autowired
    @Qualifier("oracleQueryServiceImpl")
    private QueryService oracleQueryService;

    @Override
    @Test
    void testParentAndChildQuery() throws SQLException, DBeaverMCPValidationException {
        assertParentAndChildTest(true);
    }

    @Override
    @Test
    void testCannotInsertInReadOnlyTransaction() throws DBeaverMCPValidationException {
        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', DATE '2024-03-15', TIMESTAMP '2024-03-15 14:30:45', 1);
        """;
        oracleQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.ORACLE_CONNECTION_NAME);

        List<StatementResponseDTO> responses = oracleQueryService.executeReadOnlyStatements(List.of("SELECT * FROM parent_test_entity WHERE random_string = 'abc'"), TestcontainersConfiguration.ORACLE_CONNECTION_NAME);
        assertTrue(responses.getFirst().getResponse().isEmpty());
    }

    @Override
    protected String getConnectionName() {
        return TestcontainersConfiguration.ORACLE_CONNECTION_NAME;
    }
}
