package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverDataSourcesDTO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@NullMarked
@RequiredArgsConstructor
@Service
class DBeaverDataSourceServiceImpl implements DBeaverDataSourceService {
    private final ObjectMapper objectMapper;
    private final DBeaverFilesService filesService;

    @Override
    public DBeaverDataSourcesDTO getDataSources() throws DBeaverMCPValidationException {
        String filePath = filesService.getDataSourcesFilePath();
        try {
            return objectMapper.readValue(new File(filePath), DBeaverDataSourcesDTO.class);
        } catch (JacksonException e) {
            throw new DBeaverMCPValidationException("Failed to read the \"%s\" file.".formatted(filePath), e);
        }
    }
}
