package dev.felipeflohr.dbeavermcp.module.connection.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import dev.felipeflohr.dbeavermcp.module.connection.factory.config.HikariConfigFactory;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDataDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthSSHTunnelDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionConfigurationHandlersDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionConfigurationSSHTunnelPropertiesDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverDataSourcesDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverCipherService;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.ssh.manager.SSHTunnelManager;
import dev.felipeflohr.dbeavermcp.util.JdbcUrlUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NullMarked
@RequiredArgsConstructor
@Service
class ConnectionPoolManagerImpl implements ConnectionPoolManager {
    private final HikariConfigFactory configFactory;
    private final DBeaverDataSourceService dBeaverDataSourceService;
    private final DBeaverCipherService dBeaverCipherService;
    private final SSHTunnelManager sshTunnelManager;

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    @Override
    public DataSource getDataSourceFromConnectionName(String connectionName) throws DBeaverMCPValidationException {
        String connectionIdentifier = getIdentifierFromConnectionName(connectionName);
        Map<String, DBeaverAuthConnectionDataDTO> connectionsAuth = dBeaverCipherService.getConnectionsAuthentication();
        DBeaverAuthConnectionDataDTO authData = connectionsAuth.get(connectionIdentifier);
        if (authData == null) throw new DBeaverMCPValidationException("Authentication data not found for connection %s.".formatted(connectionName));

        DBeaverConnectionDTO connectionData = dBeaverDataSourceService.getDataSources().getConnections().get(connectionIdentifier);
        String provider = connectionData.getProvider();
        String jdbcUrl = connectionData.getConfiguration().getUrl();
        String username = authData.getConnection().getUser();
        String password = authData.getConnection().getPassword();
        jdbcUrl = applySSHTunnelIfNeeded(connectionIdentifier, connectionData, authData, jdbcUrl);
        return getDataSource(provider, jdbcUrl, username, password);
    }

    @Override
    public DatabaseType getDatabaseTypeFromConnectionName(String connectionName) throws DBeaverMCPValidationException {
        String connectionIdentifier = getIdentifierFromConnectionName(connectionName);
        String provider = dBeaverDataSourceService.getDataSources().getConnections().get(connectionIdentifier).getProvider();
        Optional<DatabaseType> databaseTypeOpt = Arrays.stream(DatabaseType.values())
                .filter(d -> d.getProvider().equals(provider))
                .findFirst();
        if (databaseTypeOpt.isEmpty()) throw new DBeaverMCPValidationException("Database type not found for provider %s.".formatted(provider));
        return databaseTypeOpt.get();
    }


    private String getIdentifierFromConnectionName(String connectionName) throws DBeaverMCPValidationException {
        DBeaverDataSourcesDTO dataSources = dBeaverDataSourceService.getDataSources();
        Optional<String> connectionIdentifierOpt = dataSources.getConnections().entrySet().stream()
                .filter(e -> e.getValue().getName().equals(connectionName))
                .map(Map.Entry::getKey)
                .findFirst();
        if (connectionIdentifierOpt.isEmpty()) throw new DBeaverMCPValidationException("Connection name not found for %s.".formatted(connectionName));
        return connectionIdentifierOpt.get();
    }

    private HikariDataSource getDataSource(String provider, String jdbcUrl, String username, @Nullable String password) throws DBeaverMCPValidationException {
        String poolKey = "%s|%s|%s".formatted(provider, jdbcUrl, username);
        if (pools.containsKey(poolKey)) {
            return pools.get(poolKey);
        }

        HikariConfig config = configFactory.getHikariConfig(provider, jdbcUrl, username, password);
        var dataSource = new HikariDataSource(config);
        pools.put(poolKey, dataSource);
        return dataSource;
    }

    private String applySSHTunnelIfNeeded(
            String connectionId,
            DBeaverConnectionDTO connectionData,
            DBeaverAuthConnectionDataDTO authData,
            String jdbcUrl
    ) throws DBeaverMCPValidationException {
        DBeaverConnectionConfigurationHandlersDTO handlers = connectionData.getConfiguration().getHandlers();
        if (handlers == null || handlers.getSshTunnel() == null || !handlers.getSshTunnel().isEnabled()) {
            return jdbcUrl;
        }
        if (authData.getSshTunnel() == null) {
            throw new DBeaverMCPValidationException("SSH tunnel is enabled but no SSH credentials were found for connection %s".formatted(connectionData.getName()));
        }

        DBeaverConnectionConfigurationSSHTunnelPropertiesDTO sshProps = handlers.getSshTunnel().getProperties();
        DBeaverAuthSSHTunnelDTO sshAuth = authData.getSshTunnel();
        JdbcUrlUtils.JdbcUrlParts jdbcUrlParts = JdbcUrlUtils.extractJdbcUrlParts(jdbcUrl);
        int localPort = sshTunnelManager.openTunnel(
                connectionId,
                sshProps.getHost(),
                sshProps.getPort(),
                sshAuth.getUser(),
                sshAuth.getPassword(),
                jdbcUrlParts.host(),
                jdbcUrlParts.port()
        );

        String newJdbcUrl = JdbcUrlUtils.rewriteJdbcUrlHostAndPort(jdbcUrl, "localhost", localPort);
        log.info("Applied SSH tunnel to connection \"{}\". Previous JDBC URL: {} | Current JDBC URL: {}", connectionId, jdbcUrl, newJdbcUrl);
        return newJdbcUrl;
    }

    @PreDestroy
    private void closeAll() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }
}
