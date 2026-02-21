package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OracleQueryServiceTest extends BaseQueryServiceTest {
    @Autowired
    @Qualifier("oracleQueryServiceImpl")
    private QueryService oracleQueryService;

    @Value("classpath:scripts/type-test-oracle.sql")
    private Resource typeTestOracleScript;

    @Override
    @Test
    void testParentAndChildQuery() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        assertParentAndChildTest(true);
    }

    @Override
    @Test
    void testCannotInsertInReadOnlyTransaction() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        createParentAndChildStructure();
        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', DATE '2024-03-15', TIMESTAMP '2024-03-15 14:30:45', 1);
        """;
        DBeaverMCPValidationException exception = assertThrowsExactly(DBeaverMCPValidationException.class, () -> oracleQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.ORACLE_CONNECTION_NAME));
        assertTrue(exception.getMessage().contains("Not possible to execute query: ORA-01456: may not perform insert, delete, update operation inside a READ ONLY transaction"));
    }

    @Test
    void testTypeConversion() throws SQLException, DBeaverMCPValidationException, InterruptedException {
        testcontainersService.executeOracleScript(typeTestOracleScript);
        String sql = "SELECT * FROM all_oracle_types";
        List<StatementResponseDTO> responses = oracleQueryService.executeReadOnlyStatements(List.of(sql), getConnectionName());
        assertEquals(1, responses.size());

        StatementResponseDTO response = responses.getFirst();
        assertEquals(sql, response.getSql());

        assertEquals(1, response.getResponse().size());
        Map<String, Object> row = response.getResponse().getFirst();
        assertEquals(new BigDecimal("123456789.987654321"), row.get("COL_NUMBER"));
        assertEquals(BigDecimal.valueOf(1234567890), row.get("COL_NUMBER_P"));
        assertEquals(BigDecimal.valueOf(12345678.99), row.get("COL_NUMBER_PS"));
        assertEquals(BigDecimal.valueOf(3.14159), row.get("COL_FLOAT"));
        assertEquals(3.14f, row.get("COL_BINARY_FLOAT"));
        assertEquals(2.71828182845904, row.get("COL_BINARY_DOUBLE"));
        assertEquals(BigDecimal.valueOf(42), row.get("COL_INTEGER"));
        assertEquals(BigDecimal.valueOf(7), row.get("COL_SMALLINT"));
        assertEquals(BigDecimal.valueOf(99999.55), row.get("COL_DECIMAL"));
        assertEquals(BigDecimal.valueOf(88888.44), row.get("COL_NUMERIC"));
        assertEquals(BigDecimal.valueOf(1.618), row.get("COL_REAL"));
        assertEquals(BigDecimal.valueOf(2.302585092994046), row.get("COL_DOUBLE_PRECISION"));
        assertEquals("Fixed-length CHAR text with padding               ", row.get("COL_CHAR"));
        assertEquals("NCHAR Unicode text sample                         ", row.get("COL_NCHAR"));
        assertEquals("Variable-length VARCHAR2 text", row.get("COL_VARCHAR2"));
        assertEquals("NVARCHAR2 Unicode text sample", row.get("COL_NVARCHAR2"));
        assertEquals("Long CLOB content for testing purposes", row.get("COL_CLOB"));
        assertEquals("NCLOB Unicode content for testing purposes", row.get("COL_NCLOB"));
        assertEquals("Legacy LONG column content", row.get("COL_LONG"));
        assertEquals(LocalDateTime.of(2025, 6, 15, 14, 30, 0), row.get("COL_DATE"));
        assertEquals(LocalDateTime.of(2025, 6, 15, 14, 30, 45, 123456000), row.get("COL_TIMESTAMP"));
        assertEquals(LocalDateTime.of(2025, 6, 15, 14, 30, 45, 123000000), row.get("COL_TIMESTAMP_TZ"));
        assertEquals(LocalDateTime.of(2025, 6, 15, 14, 30, 45, 123456000), row.get("COL_TIMESTAMP_LTZ"));
        assertEquals("3-6", row.get("COL_INTERVAL_YM"));
        assertEquals("5 4:30:15.5", row.get("COL_INTERVAL_DS"));
        assertEquals("MCP converted this value to Base64 from a BLOB type: SGVsbG8gd29ybGQh", row.get("COL_BLOB"));
    }

    @Override
    protected String getConnectionName() {
        return TestcontainersConfiguration.ORACLE_CONNECTION_NAME;
    }
}
