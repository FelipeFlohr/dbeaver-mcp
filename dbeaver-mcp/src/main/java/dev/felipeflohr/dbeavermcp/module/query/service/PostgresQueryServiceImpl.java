package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Service
class PostgresQueryServiceImpl extends GenericQueryServiceImpl {
    public PostgresQueryServiceImpl(ConnectionManager poolManager, ObjectMapper objectMapper) {
        super(poolManager, objectMapper);
    }

    @Override
    protected boolean errorIsAboutInvalidIdentifier(String errorMsg) {
        boolean missingColumn = errorMsg.toLowerCase().contains("column") && errorMsg.toLowerCase().contains("does not exist");
        boolean missingTable = errorMsg.toLowerCase().contains("relation") && errorMsg.toLowerCase().contains("does not exist");
        return missingColumn || missingTable;
    }
}
