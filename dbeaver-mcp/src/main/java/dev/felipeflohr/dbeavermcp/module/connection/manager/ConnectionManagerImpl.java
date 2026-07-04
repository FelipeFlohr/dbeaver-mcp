package dev.felipeflohr.dbeavermcp.module.connection.manager;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.datasource.DBeaverMCPDatasource;
import dev.felipeflohr.dbeavermcp.module.connection.enumeration.DatabaseType;
import dev.felipeflohr.dbeavermcp.module.connection.filter.ConnectionFilter;
import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthConnectionData;
import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthSSHTunnel;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverConnection;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverConnectionConfigurationHandlers;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverConnectionConfigurationSSHTunnelProperties;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverDataSources;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverCipherService;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.ssh.manager.SSHTunnelManager;
import dev.felipeflohr.dbeavermcp.util.JdbcUrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@NullMarked
@RequiredArgsConstructor
@Service
class ConnectionManagerImpl implements ConnectionManager {
    private final DBeaverDataSourceService dBeaverDataSourceService;
    private final DBeaverCipherService dBeaverCipherService;
    private final SSHTunnelManager sshTunnelManager;
    private final ConnectionFilter connectionFilter;

    @Override
    public DataSource getDataSourceFromConnectionName(String connectionName) throws DBeaverMCPValidationException {
        String connectionIdentifier = getIdentifierFromConnectionName(connectionName);
        Map<String, DBeaverAuthConnectionData> connectionsAuth = dBeaverCipherService.getConnectionsAuthentication();
        DBeaverAuthConnectionData authData = connectionsAuth.get(connectionIdentifier);
        if (authData == null) throw new DBeaverMCPValidationException("Authentication data not found for connection %s.".formatted(connectionName));

        DBeaverConnection connectionData = dBeaverDataSourceService.getDataSources().getConnections().get(connectionIdentifier);
        String provider = connectionData.getProvider();
        String jdbcUrl = connectionData.getConfiguration().getUrl();
        String username = authData.getConnection().getUser();
        String password = Optional.ofNullable(authData.getConnection().getPassword()).orElse("");
        jdbcUrl = applySSHTunnelIfNeeded(connectionIdentifier, connectionData, authData, jdbcUrl);

        Optional<DatabaseType> databaseType = DatabaseType.fromProvider(provider);
        if (databaseType.isEmpty()) throw new DBeaverMCPValidationException("Database type not found for connection %s.".formatted(connectionName));
        try {
            return new DBeaverMCPDatasource(databaseType.get().getDriverClassName(), jdbcUrl, username, password);
        } catch (ClassNotFoundException e) {
            throw new DBeaverMCPValidationException("Driver class not found for connection %s.".formatted(connectionName));
        }
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
        if (!connectionFilter.isAllowed(connectionName)) {
            throw new DBeaverMCPValidationException("Connection \"%s\" is not accessible from this MCP instance (filtered by dbeavermcp.connections).".formatted(connectionName));
        }
        DBeaverDataSources dataSources = dBeaverDataSourceService.getDataSources();
        Optional<String> connectionIdentifierOpt = dataSources.getConnections().entrySet().stream()
                .filter(e -> e.getValue().getName().equals(connectionName))
                .map(Map.Entry::getKey)
                .findFirst();
        if (connectionIdentifierOpt.isEmpty()) throw new DBeaverMCPValidationException("Connection name not found for %s.".formatted(connectionName));
        return connectionIdentifierOpt.get();
    }

    private String applySSHTunnelIfNeeded(
            String connectionId,
            DBeaverConnection connectionData,
            DBeaverAuthConnectionData authData,
            String jdbcUrl
    ) throws DBeaverMCPValidationException {
        DBeaverConnectionConfigurationHandlers handlers = connectionData.getConfiguration().getHandlers();
        if (handlers == null || handlers.getSshTunnel() == null || !handlers.getSshTunnel().isEnabled()) {
            return jdbcUrl;
        }
        if (authData.getSshTunnel() == null) {
            throw new DBeaverMCPValidationException("SSH tunnel is enabled but no SSH credentials were found for connection %s".formatted(connectionData.getName()));
        }

        DBeaverConnectionConfigurationSSHTunnelProperties sshProps = handlers.getSshTunnel().getProperties();
        DBeaverAuthSSHTunnel sshAuth = authData.getSshTunnel();
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
}
