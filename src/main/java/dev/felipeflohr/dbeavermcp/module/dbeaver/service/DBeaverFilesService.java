package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DBeaverFilesService {
    String getDataSourcesFilePath();
    String getCredentialsConfigFilePath();
}
