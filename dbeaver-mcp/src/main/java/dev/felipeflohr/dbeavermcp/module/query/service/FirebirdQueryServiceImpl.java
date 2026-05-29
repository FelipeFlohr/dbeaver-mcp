package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;

@NullMarked
@Service
class FirebirdQueryServiceImpl extends GenericQueryServiceImpl {
    private static final String TABLE_UNKNOWN_ERROR = "42S02";
    private static final String COLUMN_UNKNOWN_ERROR = "42S22";

    public FirebirdQueryServiceImpl(ConnectionManager poolManager, ObjectMapper objectMapper) {
        super(poolManager, objectMapper);
    }

    @Override
    protected boolean errorIsAboutInvalidIdentifier(SQLException ex) {
        final String errorCode = ex.getSQLState();
        return errorCode.toLowerCase().contains(TABLE_UNKNOWN_ERROR.toLowerCase())
                || errorCode.toLowerCase().contains(COLUMN_UNKNOWN_ERROR.toLowerCase());
    }
}
