package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverDataSources;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DBeaverDataSourceService {
    DBeaverDataSources getDataSources() throws DBeaverMCPValidationException;
}
