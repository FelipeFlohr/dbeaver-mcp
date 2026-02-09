package dev.felipeflohr.dbeavermcp.module.mcp.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.query.factory.QueryServiceFactory;
import dev.felipeflohr.dbeavermcp.module.query.model.AvailableConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.module.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@SuppressWarnings("unused")
@NullMarked
@RequiredArgsConstructor
@Service
public class QueryMCPService {
    private final QueryServiceFactory queryServiceFactory;

    @McpTool(
            name = "list_available_connections",
            description = "Lists all available database connections configured in DBeaver. " +
                    "Returns connection details including identifier, name, provider (database type), and URL. " +
                    "Use this tool first to discover which connections are available before executing queries."
    )
    public List<AvailableConnectionDTO> listAvailableConnections() throws Exception {
        try {
            return queryServiceFactory.getAllAvailableConnections();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @McpTool(
            name = "execute_read_only_query",
            description = "Executes one or more read-only SQL statements against a specific database connection. " +
                    "The statements are executed within a read-only transaction that is always rolled back, " +
                    "ensuring no data is modified. Use 'list_available_connections' first to get valid connection names. " +
                    "Returns the result sets as a list of rows (maps of column name to value) for each statement that produces results."
    )
    public List<StatementResponseDTO> executeReadOnlyQuery(
            @McpToolParam(description = "The name of the database connection to execute the query against")
            String connectionName,

            @McpToolParam(description = "List of SQL statements to execute (SELECT-like queries only)")
            List<String> statements
    ) throws Exception {
        try {
            QueryService queryService = queryServiceFactory.getFromConnectionName(connectionName);
            return queryService.executeReadOnlyStatements(statements, connectionName);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private Exception handleException(Exception e) throws Exception {
        if (e instanceof DBeaverMCPValidationException dbeaverMCPException) {
            log.error("An error occurred.", e);
            throw dbeaverMCPException;
        }
        log.error("An unexpected error occurred.", e);
        throw e;
    }
}
