package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverDataSourcesDTO;
import org.jspecify.annotations.NullMarked;

/**
 * Service for dealing with DBeaver data sources.
 */
@NullMarked
public interface DBeaverDataSourceService {
    /**
     * @return {@link DBeaverDataSourcesDTO} with data source data.
     * @throws DBeaverMCPValidationException when user provided data is invalid.
     */
    DBeaverDataSourcesDTO getDataSources() throws DBeaverMCPValidationException;
}
