package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FirebirdQueryServiceTest extends BaseQueryServiceTest {
    @Autowired
    @Qualifier("firebirdQueryServiceImpl")
    private QueryService firebirdQueryService;

    @Test
    @Override
    void testParentAndChildQuery() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        assertParentAndChildTest(true);
    }

    @Test
    @Override
    void testCannotInsertInReadOnlyTransaction() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', DATE '2024-03-15', TIMESTAMP '2024-03-15 14:30:45', TRUE);
        """;
        DBeaverMCPValidationException exception = assertThrowsExactly(DBeaverMCPValidationException.class, () -> firebirdQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME));
        assertTrue(exception.getMessage().contains("Not possible to execute query: attempted update during read-only transaction [SQLState:25006, ISC error code:335544361]"));
    }

    @Override
    protected String getConnectionName() {
        return TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME;
    }
}
