package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionPoolManager;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverCipherService;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.query.factory.QueryServiceFactory;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.test.AssertionUtil;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import dev.felipeflohr.dbeavermcp.test.TestcontainersService;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@Import(TestcontainersConfiguration.class)
@SpringBootTest
abstract class BaseQueryServiceTest {
    @Autowired
    private QueryServiceFactory queryServiceFactory;

    @Autowired
    private TestcontainersService testcontainersService;

    @Autowired
    private ConnectionPoolManager connectionPoolManager;

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

    @BeforeEach
    void beforeEach() throws DBeaverMCPValidationException, SQLException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        DatabaseType databaseType = connectionPoolManager.getDatabaseTypeFromConnectionName(getConnectionName());
        switch (databaseType) {
            case POSTGRES:
                testcontainersService.executePostgresScript(parentAndChildTestPostgres);
                break;
            case ORACLE:
                testcontainersService.executeOracleScript(parentAndChildTestOracle);
                break;
            case FIREBIRD:
                testcontainersService.executeFirebirdScript(parentAndChildTestFirebird);
                break;
        }
    }

    @AfterEach
    void afterEach() throws DBeaverMCPValidationException, SQLException {
        DatabaseType databaseType = connectionPoolManager.getDatabaseTypeFromConnectionName(getConnectionName());
        switch (databaseType) {
            case POSTGRES:
                testcontainersService.clearPostgresContainer();
                break;
            case ORACLE:
                testcontainersService.clearOracleContainer();
                break;
            case FIREBIRD:
                testcontainersService.clearFirebirdContainer();
                break;
        }
    }

    @Test
    abstract void testParentAndChildQuery() throws SQLException, DBeaverMCPValidationException;

    @Test
    abstract void testCannotInsertInReadOnlyTransaction() throws DBeaverMCPValidationException;

    protected abstract String getConnectionName();

    protected void assertParentAndChildTest(boolean isUppercase) throws DBeaverMCPValidationException, SQLException {
        QueryService service = queryServiceFactory.getFromConnectionName(getConnectionName());
        String parentSql = "SELECT * FROM parent_test_entity";
        List<StatementResponseDTO> parentResponses = service.executeReadOnlyStatements(List.of(parentSql), getConnectionName());
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
        List<StatementResponseDTO> childResponses = service.executeReadOnlyStatements(List.of(childSql), getConnectionName());
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
