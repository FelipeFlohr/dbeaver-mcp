package dev.felipeflohr.dbeavermcp.module.query.service;

import dev.felipeflohr.dbeavermcp.module.connection.manager.ConnectionPoolManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.List;

@NullMarked
@Service
class OracleQueryServiceImpl extends GenericQueryServiceImpl {
    public OracleQueryServiceImpl(ConnectionPoolManager poolManager) {
        super(poolManager);
    }

    @Override
    protected List<String> statementsBeforeExecuteReadOnlyQuery() {
        //language=oracle
        String sql = "SET TRANSACTION READ ONLY";
        return List.of(sql);
    }
}
