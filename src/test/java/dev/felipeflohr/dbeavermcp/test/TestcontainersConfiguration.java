package dev.felipeflohr.dbeavermcp.test;

import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDataDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionConfigurationDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverDataSourcesDTO;
import org.firebirdsql.testcontainers.FirebirdContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    public static final String ORACLE_IDENTIFIER = "oracle";
    public static final String POSTGRES_IDENTIFIER = "postgres";
    public static final String FIREBIRD_IDENTIFIER = "firebird";

    public static final String ORACLE_CONNECTION_NAME = "Oracle connection";
    public static final String POSTGRES_CONNECTION_NAME = "Postgres connection";
    public static final String FIREBIRD_CONNECTION_NAME = "Firebird connection";

    @Bean
    DBeaverAuthConnectionDataDTO oracleAuthConnectionData(OracleContainer oracleContainer) {
        return DBeaverAuthConnectionDataDTO.builder()
                .connection(DBeaverAuthConnectionDTO.builder()
                        .user(oracleContainer.getUsername())
                        .password(oracleContainer.getPassword())
                        .build())
                .build();
    }

    @Bean
    DBeaverAuthConnectionDataDTO postgresAuthConnectionData(PostgreSQLContainer postgresContainer) {
        return DBeaverAuthConnectionDataDTO.builder()
                .connection(DBeaverAuthConnectionDTO.builder()
                        .user(postgresContainer.getUsername())
                        .password(postgresContainer.getPassword())
                        .build())
                .build();
    }

    @Bean
    DBeaverAuthConnectionDataDTO firebirdAuthConnectionData(FirebirdContainer<?> firebirdContainer) {
        return DBeaverAuthConnectionDataDTO.builder()
                .connection(DBeaverAuthConnectionDTO.builder()
                        .user(firebirdContainer.getUsername())
                        .password(firebirdContainer.getPassword())
                        .build())
                .build();
    }

    @Bean
    DBeaverDataSourcesDTO dataSources(OracleContainer oracleContainer, PostgreSQLContainer postgresContainer, FirebirdContainer<?> firebirdContainer) {
        DBeaverConnectionDTO oracleConnection = DBeaverConnectionDTO.builder()
                .name(ORACLE_CONNECTION_NAME)
                .provider("oracle")
                .driver("oracle_thin")
                .configuration(DBeaverConnectionConfigurationDTO.builder()
                        .url(oracleContainer.getJdbcUrl())
                        .build())
                .build();
        DBeaverConnectionDTO postgresConnection = DBeaverConnectionDTO.builder()
                .name(POSTGRES_CONNECTION_NAME)
                .provider("postgresql")
                .driver("postgres-jdbc")
                .configuration(DBeaverConnectionConfigurationDTO.builder()
                        .url(postgresContainer.getJdbcUrl())
                        .build())
                .build();
        DBeaverConnectionDTO firebirdConnection = DBeaverConnectionDTO.builder()
                .name(FIREBIRD_CONNECTION_NAME)
                .provider("jaybird")
                .driver("jaybird")
                .configuration(DBeaverConnectionConfigurationDTO.builder()
                        .url(firebirdContainer.getJdbcUrl())
                        .build())
                .build();
        Map<String, DBeaverConnectionDTO> connections = Map.of(
                ORACLE_IDENTIFIER, oracleConnection,
                POSTGRES_IDENTIFIER, postgresConnection,
                FIREBIRD_IDENTIFIER, firebirdConnection
        );
        return DBeaverDataSourcesDTO.builder()
                .connections(connections)
                .build();
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
}
