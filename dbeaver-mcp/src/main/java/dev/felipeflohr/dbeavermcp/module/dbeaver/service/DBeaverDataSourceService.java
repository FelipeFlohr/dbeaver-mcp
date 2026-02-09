package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverDataSourcesDTO;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DBeaverDataSourceService {
    DBeaverDataSourcesDTO getDataSources() throws DBeaverMCPValidationException;
}
