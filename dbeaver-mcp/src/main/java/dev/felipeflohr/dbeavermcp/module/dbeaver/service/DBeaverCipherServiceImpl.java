package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeaverconfig.DBeaverCipher;
import dev.felipeflohr.dbeaverconfig.data.auth.DBeaverAuthConnectionData;
import dev.felipeflohr.dbeaverconfig.data.config.DBeaverCipherConfig;
import dev.felipeflohr.dbeaverconfig.exception.DBeaverConfigException;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@NullMarked
@RequiredArgsConstructor
@Service
class DBeaverCipherServiceImpl implements DBeaverCipherService {
    private final DBeaverCipher dBeaverCipher;
    private final DBeaverFilesService filesService;

    @Value("${dbeavermcp.dbeaver.cipher.key}")
    private String key;

    @Value("${dbeavermcp.dbeaver.cipher.iv}")
    private String iv;

    @Override
    public Map<String, DBeaverAuthConnectionData> getConnectionsAuthentication() throws DBeaverMCPValidationException {
        String filePath = filesService.getCredentialsConfigFilePath();
        try {
            DBeaverCipherConfig config = new DBeaverCipherConfig(key, iv, Path.of(filePath));
            return dBeaverCipher.getConnectionsAuthentication(config);
        } catch (DBeaverConfigException e) {
            throw new DBeaverMCPValidationException("Failed to read credentials from \"%s\".".formatted(filePath), e);
        }
    }
}
