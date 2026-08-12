package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.entityparser.mcp.EntityParserMCPService;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementErrorResponseDTO;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.test.AssertionUtil;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@NullMarked
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MysqlQueryServiceTest extends BaseQueryServiceTest {
    @Autowired
    @Qualifier("mysqlQueryServiceImpl")
    private QueryService mysqlQueryService;

    @Test
    @Override
    void testParentAndChildQuery() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        assertParentAndChildTest(false);
    }

    /**
     * MySQL differs from Postgres/Oracle/Firebird: {@code Connection.setReadOnly(true)} is only a
     * routing hint in MySQL Connector/J, so an INSERT is NOT rejected at the SQL level. It executes
     * successfully within the transaction and is then rolled back by {@code GenericQueryServiceImpl}'s
     * {@code finally} block. The read-only guarantee is the rollback, not write rejection. This test
     * therefore asserts non-persistence (the inserted row did not survive the rollback) instead of
     * asserting an error message like the other dialects.
     *
     * Note: a successful statement with no result set (INSERT returns {@code false} from
     * {@code Statement.execute}) produces no {@link StatementResponseDTO} in the responses list, so
     * we assert the list is empty (i.e. no error was recorded) rather than inspecting a response.
     */
    @Test
    @Override
    void testCannotInsertInReadOnlyTransaction() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();

        String countSql = "SELECT COUNT(*) AS cnt FROM parent_test_entity";
        List<StatementResponseDTO> before = mysqlQueryService.executeReadOnlyStatements(List.of(countSql), TestcontainersConfiguration.MYSQL_CONNECTION_NAME, null);
        assertEquals(1, before.size());
        Map<String, @Nullable Object> beforeRow = before.getFirst().getResponse().getFirst();
        AssertionUtil.assertNumber(2, beforeRow.get("cnt"));

        String insertSql = "INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean) VALUES ('should_not_persist', '2024-03-15', '2024-03-15 14:30:45', TRUE)";
        List<StatementResponseDTO> insertResponses = mysqlQueryService.executeReadOnlyStatements(List.of(insertSql), TestcontainersConfiguration.MYSQL_CONNECTION_NAME, null);
        assertTrue(insertResponses.isEmpty(), "INSERT should have succeeded without a result set and without recording an error");

        List<StatementResponseDTO> after = mysqlQueryService.executeReadOnlyStatements(List.of(countSql), TestcontainersConfiguration.MYSQL_CONNECTION_NAME, null);
        assertEquals(1, after.size());
        Map<String, @Nullable Object> afterRow = after.getFirst().getResponse().getFirst();
        AssertionUtil.assertNumber(2, afterRow.get("cnt"));
    }

    @Test
    void testInvalidIdentifierReturnsHintWhenQueryingUnexistentColumn() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        String sql = "SELECT just_a_column_trust_me FROM parent_test_entity";
        List<StatementResponseDTO> responses = mysqlQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.MYSQL_CONNECTION_NAME, null);

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
        List<StatementResponseDTO> responses = mysqlQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.MYSQL_CONNECTION_NAME, null);

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
        String sql = "SELECT SLEEP(10)";
        long start = System.currentTimeMillis();
        List<StatementResponseDTO> responses = mysqlQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.MYSQL_CONNECTION_NAME, 1);
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
        return TestcontainersConfiguration.MYSQL_CONNECTION_NAME;
    }
}
