package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Service
class FirebirdQueryServiceImpl extends GenericQueryServiceImpl {
    public FirebirdQueryServiceImpl(ConnectionManager poolManager, ObjectMapper objectMapper) {
        super(poolManager, objectMapper);
    }

    @Override
    protected boolean errorIsAboutInvalidIdentifier(String errorMsg) {
        return errorMsg.toLowerCase().contains("Column unknown".toLowerCase())
                || errorMsg.toLowerCase().contains("Table unknown".toLowerCase());
    }
}
