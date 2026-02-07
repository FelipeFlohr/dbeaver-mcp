package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionPoolManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@NullMarked
@Service
class OracleQueryServiceImpl extends GenericQueryServiceImpl {
    public OracleQueryServiceImpl(ConnectionPoolManager poolManager) {
        super(poolManager);
    }
}
