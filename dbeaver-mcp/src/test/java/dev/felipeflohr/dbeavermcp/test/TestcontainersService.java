package dev.felipeflohr.dbeavermcp.test;

import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthConnectionData;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverDataSources;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverCipherService;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import org.firebirdsql.testcontainers.FirebirdContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@Service
@ConditionalOnBean({ PostgreSQLContainer.class, OracleContainer.class, FirebirdContainer.class, MySQLContainer.class })
public class TestcontainersService {
    private static final Logger log = LoggerFactory.getLogger(TestcontainersService.class);
    private final PostgreSQLContainer postgresContainer;
    private final OracleContainer oracleContainer;
    private final FirebirdContainer<?> firebirdContainer;
    private final MySQLContainer mysqlContainer;
    private final DBeaverDataSources dataSources;
    private final DBeaverAuthConnectionData oracleAuthConnectionData;
    private final DBeaverAuthConnectionData postgresAuthConnectionData;
    private final DBeaverAuthConnectionData firebirdAuthConnectionData;
    private final DBeaverAuthConnectionData mysqlAuthConnectionData;

    public TestcontainersService(PostgreSQLContainer postgresContainer, OracleContainer oracleContainer, FirebirdContainer<?> firebirdContainer, MySQLContainer mysqlContainer, DBeaverDataSources dataSources, DBeaverAuthConnectionData oracleAuthConnectionData, DBeaverAuthConnectionData postgresAuthConnectionData, DBeaverAuthConnectionData firebirdAuthConnectionData, DBeaverAuthConnectionData mysqlAuthConnectionData) {
        this.postgresContainer = postgresContainer;
        this.oracleContainer = oracleContainer;
        this.firebirdContainer = firebirdContainer;
        this.mysqlContainer = mysqlContainer;
        this.dataSources = dataSources;
        this.oracleAuthConnectionData = oracleAuthConnectionData;
        this.postgresAuthConnectionData = postgresAuthConnectionData;
        this.firebirdAuthConnectionData = firebirdAuthConnectionData;
        this.mysqlAuthConnectionData = mysqlAuthConnectionData;
    }

    public void mockDBeaverConnections(DBeaverDataSourceService mockedDBeaverDataSourceService, DBeaverCipherService mockedDBeaverCipherService) throws DBeaverMCPValidationException {
        when(mockedDBeaverDataSourceService.getDataSources()).thenReturn(dataSources);
        when(mockedDBeaverCipherService.getConnectionsAuthentication()).thenReturn(Map.of(
                TestcontainersConfiguration.ORACLE_IDENTIFIER, oracleAuthConnectionData,
                TestcontainersConfiguration.POSTGRES_IDENTIFIER, postgresAuthConnectionData,
                TestcontainersConfiguration.FIREBIRD_IDENTIFIER, firebirdAuthConnectionData,
                TestcontainersConfiguration.MYSQL_IDENTIFIER, mysqlAuthConnectionData
        ));
    }

    public void executePostgresScript(Resource script) throws SQLException {
        try (Connection connection = getConnection(postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword())) {
            ScriptUtils.executeSqlScript(connection, script);
        }
    }

    public void executeOracleScript(Resource script) throws SQLException, InterruptedException {
        try (Connection connection = getConnection(oracleContainer.getJdbcUrl(), oracleContainer.getUsername(), oracleContainer.getPassword())) {
            ScriptUtils.executeSqlScript(connection, script);
            Thread.sleep(1500); // Necessary because of the ORA-01466 error
        }
    }

    public void executeFirebirdScript(Resource script) throws SQLException {
        try (Connection connection = getConnection(firebirdContainer.getJdbcUrl(), firebirdContainer.getUsername(), firebirdContainer.getPassword())) {
            ScriptUtils.executeSqlScript(connection, script);
        }
    }

    public void executeMysqlScript(Resource script) throws SQLException {
        try (Connection connection = getConnection(mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(), mysqlContainer.getPassword())) {
            ScriptUtils.executeSqlScript(connection, script);
        }
    }

    public void clearPostgresContainer() throws SQLException {
        Connection connection = getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword()
        );

        try (Statement statement = connection.createStatement()) {
            ResultSet tables = statement.executeQuery(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
            );

            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add("\"" + tables.getString("tablename") + "\"");
            }
            tables.close();

            for (String tableName : tableNames) {
                statement.execute("DROP TABLE IF EXISTS " + tableName + " CASCADE");
            }
        }
        log.info("Cleared Postgres database.");
    }

    public void clearOracleContainer() throws SQLException {
        Connection connection = getConnection(oracleContainer.getJdbcUrl(), oracleContainer.getUsername(), oracleContainer.getPassword());
        try (Statement statement = connection.createStatement()) {
            String sql = """
                    Begin
                        for c in (select table_name from user_tables) loop
                            execute immediate ('drop table ' ||c.table_name|| ' cascade constraints');
                        end loop;
                    End;""";
            statement.execute(sql);
        }
        log.info("Cleared Oracle database.");
    }

    public void clearFirebirdContainer() throws SQLException {
        Connection connection = getConnection(
                firebirdContainer.getJdbcUrl(),
                firebirdContainer.getUsername(),
                firebirdContainer.getPassword()
        );

        try (Statement statement = connection.createStatement()) {
            ResultSet tables = statement.executeQuery(
                    "SELECT TRIM(RDB$RELATION_NAME) AS TABLE_NAME FROM RDB$RELATIONS " +
                            "WHERE RDB$SYSTEM_FLAG = 0 AND RDB$VIEW_BLR IS NULL"
            );

            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }
            tables.close();

            int maxAttempts = tableNames.size() + 1;
            List<String> remaining = new ArrayList<>(tableNames);

            for (int i = 0; i < maxAttempts && !remaining.isEmpty(); i++) {
                Iterator<String> iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    String tableName = iterator.next();
                    try {
                        statement.execute("DROP TABLE \"" + tableName + "\"");
                        iterator.remove();
                    } catch (SQLException _) {}
                }
            }
        }
        log.info("Cleared Firebird database.");
    }

    public void clearMysqlContainer() throws SQLException {
        Connection connection = getConnection(
                mysqlContainer.getJdbcUrl(),
                mysqlContainer.getUsername(),
                mysqlContainer.getPassword()
        );

        try (Statement statement = connection.createStatement()) {
            ResultSet tables = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
            );

            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("table_name"));
            }
            tables.close();

            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String tableName : tableNames) {
                statement.execute("DROP TABLE IF EXISTS `" + tableName + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        log.info("Cleared MySQL database.");
    }

    private Connection getConnection(String jdbcUrl, String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
