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
class PostgresQueryServiceTest extends BaseQueryServiceTest {
    @Autowired
    @Qualifier("postgresQueryServiceImpl")
    private QueryService postgresQueryService;

    @Override
    @Test
    void testParentAndChildQuery() throws SQLException, DBeaverMCPValidationException {
        assertParentAndChildTest(false);
    }

    @Override
    @Test
    void testCannotInsertInReadOnlyTransaction() {
        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', '2024-03-15', '2024-03-15 14:30:45', TRUE)
        """;
        DBeaverMCPValidationException exception = assertThrowsExactly(DBeaverMCPValidationException.class, () -> postgresQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.POSTGRES_CONNECTION_NAME));
        assertTrue(exception.getMessage().contains("Not possible to execute query: ERROR: cannot execute INSERT in a read-only transaction"));
    }

    @Override
    protected String getConnectionName() {
        return TestcontainersConfiguration.POSTGRES_CONNECTION_NAME;
    }
}
