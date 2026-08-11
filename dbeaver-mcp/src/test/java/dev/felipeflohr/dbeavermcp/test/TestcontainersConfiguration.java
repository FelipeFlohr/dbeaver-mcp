package dev.felipeflohr.dbeavermcp.test;

import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthConnection;
import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthConnectionData;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverConnection;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverConnectionConfiguration;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverDataSources;
import org.firebirdsql.testcontainers.FirebirdContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    public static final String ORACLE_IDENTIFIER = "oracle";
    public static final String POSTGRES_IDENTIFIER = "postgres";
    public static final String FIREBIRD_IDENTIFIER = "firebird";
    public static final String MYSQL_IDENTIFIER = "mysql";

    public static final String ORACLE_CONNECTION_NAME = "Oracle connection";
    public static final String POSTGRES_CONNECTION_NAME = "Postgres connection";
    public static final String FIREBIRD_CONNECTION_NAME = "Firebird connection";
    public static final String MYSQL_CONNECTION_NAME = "MySQL connection";

    @Bean
    DBeaverAuthConnectionData oracleAuthConnectionData(OracleContainer oracleContainer) {
        return authConnectionData(oracleContainer.getUsername(), oracleContainer.getPassword());
    }

    @Bean
    DBeaverAuthConnectionData postgresAuthConnectionData(PostgreSQLContainer postgresContainer) {
        return authConnectionData(postgresContainer.getUsername(), postgresContainer.getPassword());
    }

    @Bean
    DBeaverAuthConnectionData firebirdAuthConnectionData(FirebirdContainer<?> firebirdContainer) {
        return authConnectionData(firebirdContainer.getUsername(), firebirdContainer.getPassword());
    }

    @Bean
    DBeaverAuthConnectionData mysqlAuthConnectionData(MySQLContainer mysqlContainer) {
        return authConnectionData(mysqlContainer.getUsername(), mysqlContainer.getPassword());
    }

    @Bean
    DBeaverDataSources dataSources(OracleContainer oracleContainer, PostgreSQLContainer postgresContainer, FirebirdContainer<?> firebirdContainer, MySQLContainer mysqlContainer) {
        DBeaverConnection oracleConnection = connection(ORACLE_CONNECTION_NAME, "oracle", "oracle_thin", oracleContainer.getJdbcUrl());
        DBeaverConnection postgresConnection = connection(POSTGRES_CONNECTION_NAME, "postgresql", "postgres-jdbc", postgresContainer.getJdbcUrl());
        DBeaverConnection firebirdConnection = connection(FIREBIRD_CONNECTION_NAME, "jaybird", "jaybird", firebirdContainer.getJdbcUrl());
        DBeaverConnection mysqlConnection = connection(MYSQL_CONNECTION_NAME, "mysql", "mysql", mysqlContainer.getJdbcUrl());

        DBeaverDataSources dataSources = new DBeaverDataSources();
        dataSources.setConnections(Map.of(
                ORACLE_IDENTIFIER, oracleConnection,
                POSTGRES_IDENTIFIER, postgresConnection,
                FIREBIRD_IDENTIFIER, firebirdConnection,
                MYSQL_IDENTIFIER, mysqlConnection
        ));
        return dataSources;
    }

    private static DBeaverAuthConnectionData authConnectionData(String user, String password) {
        DBeaverAuthConnection connection = new DBeaverAuthConnection();
        connection.setUser(user);
        connection.setPassword(password);

        DBeaverAuthConnectionData authConnectionData = new DBeaverAuthConnectionData();
        authConnectionData.setConnection(connection);
        return authConnectionData;
    }

    private static DBeaverConnection connection(String name, String provider, String driver, String jdbcUrl) {
        DBeaverConnectionConfiguration configuration = new DBeaverConnectionConfiguration();
        configuration.setUrl(jdbcUrl);

        DBeaverConnection connection = new DBeaverConnection();
        connection.setName(name);
        connection.setProvider(provider);
        connection.setDriver(driver);
        connection.setConfiguration(configuration);
        return connection;
    }

    @Bean
    @ServiceConnection
    OracleContainer oracleFreeContainer() {
        return new OracleContainer(DockerImageName.parse("gvenzl/oracle-free").withTag("latest-faststart"));
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres").withTag("latest"));
    }

    @Bean
    @ServiceConnection
    FirebirdContainer<?> firebirdContainer() {
        return new FirebirdContainer<>(DockerImageName.parse(FirebirdContainer.IMAGE).withTag("5.0.3"));
    }

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql").withTag("8.4"));
    }
}
