package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.entityparser.mcp.EntityParserMCPService;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementErrorResponseDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        List<StatementResponseDTO> responses = firebirdQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME, null);
        assertEquals(1, responses.size());
        assertEquals(sql, responses.getFirst().getSql());
        assertNull(responses.getFirst().getResponse());
        assertNotNull(responses.getFirst().getError());
        StatementErrorResponseDTO error = responses.getFirst().getError();
        assertTrue(error.getMessage().contains("attempted update during read-only transaction"));
    }

    @Test
    void testInvalidIdentifierReturnsHintWhenQueryingUnexistentColumn() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        String sql = "SELECT just_a_column_trust_me FROM parent_test_entity";
        List<StatementResponseDTO> responses = firebirdQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME, null);

        assertEquals(1, responses.size());
        assertEquals(sql, responses.getFirst().getSql());
        assertNull(responses.getFirst().getResponse());
        assertNotNull(responses.getFirst().getError());

        StatementErrorResponseDTO error = responses.getFirst().getError();
        assertNotNull(error.getHint());
        String expectedHint = "You have an invalid identifier in your SQL. To avoid expend unnecessary tokens and time, try " +
                "using the \"%s\" MCP tool.".formatted(EntityParserMCPService.GET_TABLE_COLUMNS_FROM_JPA_ENTITY_TOOL_NAME);
        assertEquals(expectedHint, error.getHint());
    }

    @Test
    void testInvalidIdentifierReturnsHintWhenQueryingUnexistentTable() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        String sql = "SELECT just_a_column_trust_me FROM just_a_table_trust_me";
        List<StatementResponseDTO> responses = firebirdQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME, null);

        assertEquals(1, responses.size());
        assertEquals(sql, responses.getFirst().getSql());
        assertNull(responses.getFirst().getResponse());
        assertNotNull(responses.getFirst().getError());

        StatementErrorResponseDTO error = responses.getFirst().getError();
        assertNotNull(error.getHint());
        String expectedHint = "You have an invalid identifier in your SQL. To avoid expend unnecessary tokens and time, try " +
                "using the \"%s\" MCP tool.".formatted(EntityParserMCPService.GET_TABLE_COLUMNS_FROM_JPA_ENTITY_TOOL_NAME);
        assertEquals(expectedHint, error.getHint());
    }

    @Test
    void testTimeoutIsEnforced() throws DBeaverMCPValidationException {
        String sql = """
                WITH RECURSIVE gen(n) AS (
                    SELECT 1 FROM rdb$database
                    UNION ALL
                    SELECT n + 1 FROM gen WHERE n < 5000000
                )
                SELECT COUNT(*) FROM gen
                """;
        long start = System.currentTimeMillis();
        List<StatementResponseDTO> responses = firebirdQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME, 1);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 5000, "Timeout should have interrupted the query, but it took " + elapsed + "ms");
        assertEquals(1, responses.size());
        StatementResponseDTO response = responses.getFirst();
        assertEquals(sql, response.getSql());
        assertNull(response.getResponse());
        assertNotNull(response.getError());
    }

    @Override
    protected String getConnectionName() {
        return TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME;
    }
}
