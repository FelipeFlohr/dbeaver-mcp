package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverCipherService;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.query.factory.QueryServiceFactory;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.test.AssertionUtil;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import dev.felipeflohr.dbeavermcp.test.TestcontainersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class QueryServiceTest {
    @Autowired
    private QueryServiceFactory queryServiceFactory;

    @Autowired
    @Qualifier("postgresQueryServiceImpl")
    private QueryService postgresQueryService;

    @Autowired
    @Qualifier("oracleQueryServiceImpl")
    private QueryService oracleQueryService;

    @Autowired
    @Qualifier("firebirdQueryServiceImpl")
    private QueryService firebirdQueryService;

    @Autowired
    private TestcontainersService testcontainersService;

    @MockitoBean
    private DBeaverDataSourceService mockedDBeaverDataSourceService;

    @MockitoBean
    private DBeaverCipherService mockedDBeaverCipherService;

    @Value("classpath:scripts/parent-and-child-test-postgres.sql")
    private Resource parentAndChildTestPostgres;

    @Value("classpath:scripts/parent-and-child-test-oracle.sql")
    private Resource parentAndChildTestOracle;

    @Value("classpath:scripts/parent-and-child-test-firebird.sql")
    private Resource parentAndChildTestFirebird;

    @Test
    void queryPostgres() throws SQLException, DBeaverMCPValidationException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.clearPostgresContainer();
        testcontainersService.executePostgresScript(parentAndChildTestPostgres);
        assertParentAndChildTest(TestcontainersConfiguration.POSTGRES_CONNECTION_NAME, false);
    }

    @Test
    void queryOracle() throws SQLException, DBeaverMCPValidationException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.clearOracleContainer();
        testcontainersService.executeOracleScript(parentAndChildTestOracle);
        assertParentAndChildTest(TestcontainersConfiguration.ORACLE_CONNECTION_NAME, true);
    }

    @Test
    void queryFirebird() throws SQLException, DBeaverMCPValidationException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.clearFirebirdContainer();
        testcontainersService.executeFirebirdScript(parentAndChildTestFirebird);
        assertParentAndChildTest(TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME, true);
    }

    @Test
    void postgresCannotInsertInReadOnlyTransaction() throws DBeaverMCPValidationException, SQLException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.clearPostgresContainer();
        testcontainersService.executePostgresScript(parentAndChildTestPostgres);

        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', '2024-03-15', '2024-03-15 14:30:45', TRUE)
        """;
        DBeaverMCPValidationException exception = assertThrowsExactly(DBeaverMCPValidationException.class, () -> postgresQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.POSTGRES_CONNECTION_NAME));
        assertTrue(exception.getMessage().contains("Not possible to execute query: ERROR: cannot execute INSERT in a read-only transaction"));
    }

    @Test
    void oracleCannotInsertInReadOnlyTransaction() throws DBeaverMCPValidationException, SQLException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.clearOracleContainer();
        testcontainersService.executeOracleScript(parentAndChildTestOracle);

        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', DATE '2024-03-15', TIMESTAMP '2024-03-15 14:30:45', 1);
        """;
        DBeaverMCPValidationException exception = assertThrowsExactly(DBeaverMCPValidationException.class, () -> oracleQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.ORACLE_CONNECTION_NAME));
        assertTrue(exception.getMessage().contains("Not possible to execute query: ORA-01456: may not perform insert, delete, update operation inside a READ ONLY transaction"));
    }

    @Test
    void firebirdCannotInsertInReadOnlyTransaction() throws DBeaverMCPValidationException, SQLException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.clearFirebirdContainer();
        testcontainersService.executeFirebirdScript(parentAndChildTestFirebird);

        String sql = """
                INSERT INTO parent_test_entity (random_string, random_date, random_date_time, random_boolean)
                VALUES ('abc', DATE '2024-03-15', TIMESTAMP '2024-03-15 14:30:45', TRUE);
        """;
        DBeaverMCPValidationException exception = assertThrowsExactly(DBeaverMCPValidationException.class, () -> firebirdQueryService.executeReadOnlyStatements(List.of(sql), TestcontainersConfiguration.FIREBIRD_CONNECTION_NAME));
        assertTrue(exception.getMessage().contains("Not possible to execute query: attempted update during read-only transaction [SQLState:25006, ISC error code:335544361]"));
    }

    private void assertParentAndChildTest(String connectionName, boolean isUppercase) throws DBeaverMCPValidationException, SQLException {
        QueryService service = queryServiceFactory.getFromConnectionName(connectionName);
        String parentSql = "SELECT * FROM parent_test_entity";
        List<StatementResponseDTO> parentResponses = service.executeReadOnlyStatements(List.of(parentSql), connectionName);
        assertEquals(1, parentResponses.size());
        StatementResponseDTO parentResponse = parentResponses.getFirst();
        assertEquals(parentSql, parentResponse.getSql());
        assertEquals(2, parentResponse.getResponse().size());

        final String idColumn = isUppercase ? "ID" : "id";
        final String randomStringColumn = isUppercase ? "RANDOM_STRING" : "random_string";
        final String randomDateColumn = isUppercase ? "RANDOM_DATE" : "random_date";
        final String randomDateTimeColumn = isUppercase ? "RANDOM_DATE_TIME" : "random_date_time";
        final String randomBooleanColumn = isUppercase ? "RANDOM_BOOLEAN" : "random_boolean";
        final String parentColumn = isUppercase ? "PARENT" : "parent";

        Map<String, Object> firstParentRow = parentResponse.getResponse().getFirst();
        AssertionUtil.assertNumber(1, firstParentRow.get(idColumn));
        assertEquals("xK9mPqL2vNwR7tYs", firstParentRow.get(randomStringColumn));
        AssertionUtil.assertDate(LocalDate.of(2024, 3, 15), firstParentRow.get(randomDateColumn));
        AssertionUtil.assertDateTime(LocalDateTime.of(2024, 3, 15, 14, 30, 45), firstParentRow.get(randomDateTimeColumn));
        AssertionUtil.assertTrue(firstParentRow.get(randomBooleanColumn));

        Map<String, Object> secondParentRow = parentResponse.getResponse().getLast();
        AssertionUtil.assertNumber(2, secondParentRow.get(idColumn));
        assertEquals("bH4jFcA8eZuW1oMn", secondParentRow.get(randomStringColumn));
        AssertionUtil.assertDate(LocalDate.of(2024, 7, 22), secondParentRow.get(randomDateColumn));
        AssertionUtil.assertDateTime(LocalDateTime.of(2024, 7, 22, 9, 15, 30), secondParentRow.get(randomDateTimeColumn));
        AssertionUtil.assertFalse(secondParentRow.get(randomBooleanColumn));

        String childSql = "SELECT * FROM child_test_entity";
        List<StatementResponseDTO> childResponses = service.executeReadOnlyStatements(List.of(childSql), connectionName);
        assertEquals(1, childResponses.size());
        StatementResponseDTO childResponse = childResponses.getFirst();
        assertEquals(childSql, childResponse.getSql());
        assertEquals(4, childResponse.getResponse().size());

        Map<String, Object> firstChildRow = childResponse.getResponse().getFirst();
        AssertionUtil.assertNumber(1, firstChildRow.get(idColumn));
        assertEquals("qT6yRpD3sVxL9wKm", firstChildRow.get(randomStringColumn));
        AssertionUtil.assertNumber(1, firstChildRow.get(parentColumn));

        Map<String, Object> secondChildRow = childResponse.getResponse().get(1);
        AssertionUtil.assertNumber(2, secondChildRow.get(idColumn));
        assertEquals("nG2hJcB7fZuE4oAi", secondChildRow.get(randomStringColumn));
        AssertionUtil.assertNumber(1, secondChildRow.get(parentColumn));

        Map<String, Object> thirdChildRow = childResponse.getResponse().get(2);
        AssertionUtil.assertNumber(3, thirdChildRow.get(idColumn));
        assertEquals("mW5kPtC1vNxQ8rYs", thirdChildRow.get(randomStringColumn));
        AssertionUtil.assertNumber(2, thirdChildRow.get(parentColumn));

        Map<String, Object> fourthChildRow = childResponse.getResponse().getLast();
        AssertionUtil.assertNumber(4, fourthChildRow.get(idColumn));
        assertEquals("dL3jHbF9eZaU6oXn", fourthChildRow.get(randomStringColumn));
        AssertionUtil.assertNumber(2, fourthChildRow.get(parentColumn));
    }

}
