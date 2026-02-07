package dev.felipeflohr.dbeavermcp.test;

import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverFilesService;
import org.jspecify.annotations.NullMarked;

import static org.mockito.Mockito.*;

@NullMarked
public class MockDBeaverFilesUtil {
    private MockDBeaverFilesUtil() {}

    public static void mockFilesDataSourcesFilePath(DBeaverFilesService service, String dataSourcesFilePath) {
        when(service.getDataSourcesFilePath()).thenReturn(dataSourcesFilePath);
    }

    public static void mockFilesCredentialsConfigFilePath(DBeaverFilesService service, String credentialsConfigFilePath) {
        when(service.getCredentialsConfigFilePath()).thenReturn(credentialsConfigFilePath);
    }
}
