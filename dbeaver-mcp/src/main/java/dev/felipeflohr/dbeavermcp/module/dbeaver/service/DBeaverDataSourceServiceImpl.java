package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeaverconfig.DBeaverDataSource;
import dev.felipeflohr.dbeaverconfig.data.config.DBeaverDataSourceConfig;
import dev.felipeflohr.dbeaverconfig.data.datasource.DBeaverDataSources;
import dev.felipeflohr.dbeaverconfig.exception.DBeaverConfigException;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@NullMarked
@RequiredArgsConstructor
@Service
class DBeaverDataSourceServiceImpl implements DBeaverDataSourceService {
    private final DBeaverDataSource dBeaverDataSource;
    private final DBeaverFilesService filesService;

    @Override
    public DBeaverDataSources getDataSources() throws DBeaverMCPValidationException {
        String filePath = filesService.getDataSourcesFilePath();
        try {
            DBeaverDataSourceConfig config = new DBeaverDataSourceConfig(Path.of(filePath));
            return dBeaverDataSource.getDataSources(config);
        } catch (DBeaverConfigException e) {
            throw new DBeaverMCPValidationException("Failed to read the \"%s\" file.".formatted(filePath), e);
        }
    }
}
