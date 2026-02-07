package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.module.system.service.SystemService;
import dev.felipeflohr.dbeavermcp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@NullMarked
@RequiredArgsConstructor
@Service
class DBeaverFilesServiceImpl implements DBeaverFilesService {
    private static final String DBEAVER_RELATIVE_PATH = Path.of("DBeaverData", "workspace6", "General", ".dbeaver").toString();

    private final SystemService systemService;

    @Value("${dbeavermcp.dbeaver.config.data-sources-file-path}")
    private String dataSourcesFilePath;

    @Value("${dbeavermcp.dbeaver.config.credentials-config-file-path}")
    private String credentialsConfigFilePath;

    @Override
    public String getDataSourcesFilePath() {
        if (StringUtils.isNotBlank(dataSourcesFilePath)) return dataSourcesFilePath;
        return getDefaultDBeaverDir().resolve("data-sources.json").toString();
    }

    @Override
    public String getCredentialsConfigFilePath() {
        if (StringUtils.isNotBlank(credentialsConfigFilePath)) return credentialsConfigFilePath;
        return getDefaultDBeaverDir().resolve("credentials-config.json").toString();
    }

    private Path getDefaultDBeaverDir() {
        Path base = switch (systemService.getOS()) {
            case WINDOWS -> Path.of(System.getenv("APPDATA"));
            case MAC -> Path.of(System.getProperty("user.home"), "Library", "DBeaverData");
            case LINUX -> Path.of(System.getProperty("user.home"), ".local", "share");
        };
        return base.resolve(DBEAVER_RELATIVE_PATH);
    }
}
