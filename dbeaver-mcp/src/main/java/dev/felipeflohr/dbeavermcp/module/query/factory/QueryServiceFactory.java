package dev.felipeflohr.dbeavermcp.module.query.factory;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionConfigurationHandlersDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionConfigurationSSHTunnelHandlerDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionConfigurationSSHTunnelPropertiesDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.query.model.AvailableConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@NullMarked
@RequiredArgsConstructor
@Component
public class QueryServiceFactory {
    private static final String SSH_AUTH_TYPE = "PASSWORD";
    private final QueryService postgresQueryServiceImpl;
    private final QueryService oracleQueryServiceImpl;
    private final QueryService firebirdQueryServiceImpl;
    private final ConnectionManager connectionManager;
    private final DBeaverDataSourceService dBeaverDataSourceService;

    public QueryService getFromConnectionName(String connectionName) throws DBeaverMCPValidationException {
        return switch (connectionManager.getDatabaseTypeFromConnectionName(connectionName)) {
            case POSTGRES -> postgresQueryServiceImpl;
            case ORACLE -> oracleQueryServiceImpl;
            case FIREBIRD -> firebirdQueryServiceImpl;
        };
    }

    public List<AvailableConnectionDTO> getAllAvailableConnections() throws DBeaverMCPValidationException {
        return dBeaverDataSourceService.getDataSources().getConnections().entrySet().stream()
                .filter(e -> {
                    DBeaverConnectionConfigurationSSHTunnelPropertiesDTO sshProperties = Optional.ofNullable(e.getValue().getConfiguration().getHandlers())
                            .map(DBeaverConnectionConfigurationHandlersDTO::getSshTunnel)
                            .map(DBeaverConnectionConfigurationSSHTunnelHandlerDTO::getProperties)
                            .orElse(null);
                    if (sshProperties == null) return true;
                    if (!SSH_AUTH_TYPE.equals(sshProperties.getAuthType())) {
                        log.warn("SSH Auth Type {} is not supported. Connection \"{}\"", sshProperties.getAuthType(), e.getValue().getName());
                        return false;
                    }
                    return true;
                })
                .map(e -> {
                    String sshHost = null;
                    Integer sshPort = null;
                    DBeaverConnectionConfigurationSSHTunnelHandlerDTO sshTunnelHandler = Optional.ofNullable(e.getValue().getConfiguration().getHandlers())
                            .map(DBeaverConnectionConfigurationHandlersDTO::getSshTunnel)
                            .orElse(null);
                    if (sshTunnelHandler != null) {
                        sshHost = sshTunnelHandler.getProperties().getHost();
                        sshPort = sshTunnelHandler.getProperties().getPort();
                    }

                    return AvailableConnectionDTO.builder()
                            .identifier(e.getKey())
                            .url(e.getValue().getConfiguration().getUrl())
                            .provider(e.getValue().getProvider())
                            .name(e.getValue().getName())
                            .sshHost(sshHost)
                            .sshPort(sshPort)
                            .build();
                })
                .toList();
    }
}
