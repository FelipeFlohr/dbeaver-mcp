package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@NullMarked
@Service
class FirebirdQueryServiceImpl extends GenericQueryServiceImpl {
    public FirebirdQueryServiceImpl(ConnectionManager poolManager) {
        super(poolManager);
    }
}
