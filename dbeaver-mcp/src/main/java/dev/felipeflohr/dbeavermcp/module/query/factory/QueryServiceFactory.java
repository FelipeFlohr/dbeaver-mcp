package dev.felipeflohr.dbeavermcp.module.query.factory;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionPoolManager;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.query.model.AvailableConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import java.util.List;

@NullMarked
@RequiredArgsConstructor
@Component
public class QueryServiceFactory {
    private final QueryService postgresQueryServiceImpl;
    private final QueryService oracleQueryServiceImpl;
    private final QueryService firebirdQueryServiceImpl;
    private final ConnectionPoolManager connectionPoolManager;
    private final DBeaverDataSourceService dBeaverDataSourceService;

    public QueryService getFromConnectionName(String connectionName) throws DBeaverMCPValidationException {
        return switch (connectionPoolManager.getDatabaseTypeFromConnectionName(connectionName)) {
            case POSTGRES -> postgresQueryServiceImpl;
            case ORACLE -> oracleQueryServiceImpl;
            case FIREBIRD -> firebirdQueryServiceImpl;
        };
    }

    public List<AvailableConnectionDTO> getAllAvailableConnections() throws DBeaverMCPValidationException {
        return dBeaverDataSourceService.getDataSources().getConnections().entrySet().stream()
                .map(e -> AvailableConnectionDTO.builder()
                        .identifier(e.getKey())
                        .url(e.getValue().getConfiguration().getUrl())
                        .provider(e.getValue().getProvider())
                        .name(e.getValue().getName())
                        .build())
                .toList();
    }
}
