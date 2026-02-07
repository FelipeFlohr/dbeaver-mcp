package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDataDTO;
import dev.felipeflohr.dbeavermcp.test.MockDBeaverFilesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class DBeaverCipherServiceTest {

    @Autowired
    private DBeaverCipherService service;

    @Value("classpath:credentials-config.json")
    private Resource credentialsFile;

    @MockitoBean
    private DBeaverFilesService filesService;

    @BeforeEach
    void beforeEach() throws IOException {
        MockDBeaverFilesUtil.mockFilesCredentialsConfigFilePath(filesService, credentialsFile.getFilePath().toAbsolutePath().toString());
    }

    @Test
    void decryptFile() throws DBeaverMCPValidationException {
        final String postgresConnectionWithoutSsh = "postgres-jdbc-196079609f2-53d190edd595caaa";
        final String postgresConnectionWithSsh = "postgres-jdbc-19bc99acaa1-78b516adfdeb47d9";
        final String oracleConnectionWithNormalLogon = "oracle_thin-19c2146e52e-70ea308a7473e0a9";
        final String oracleConnectionWithSysdbaLogon = "oracle_thin-19c2147fa39-6d55b3e69d72ab41";

        Map<String, DBeaverAuthConnectionDataDTO> connectionMap = service.getConnectionsAuthentication();
        assertTrue(connectionMap.containsKey(postgresConnectionWithoutSsh));
        assertTrue(connectionMap.containsKey(postgresConnectionWithSsh));
        assertTrue(connectionMap.containsKey(oracleConnectionWithNormalLogon));
        assertTrue(connectionMap.containsKey(oracleConnectionWithSysdbaLogon));
        DBeaverAuthConnectionDataDTO withoutSshData = connectionMap.get(postgresConnectionWithoutSsh);
        DBeaverAuthConnectionDataDTO withSshData = connectionMap.get(postgresConnectionWithSsh);
        DBeaverAuthConnectionDataDTO oracleWithNormalLogon = connectionMap.get(oracleConnectionWithNormalLogon);
        DBeaverAuthConnectionDataDTO oracleWithSysdbaLogon = connectionMap.get(oracleConnectionWithSysdbaLogon);

        assertEquals("admin", withoutSshData.getConnection().getUser());
        assertEquals("admin", withoutSshData.getConnection().getPassword());
        assertNull(withoutSshData.getConnection().getOracleLogonAs());

        assertEquals("admin", withSshData.getConnection().getUser());
        assertNull(withSshData.getConnection().getPassword());
        assertNull(withSshData.getConnection().getOracleLogonAs());
        assertNotNull(withSshData.getSshTunnel());
        assertEquals("username", withSshData.getSshTunnel().getUser());
        assertEquals("password", withSshData.getSshTunnel().getPassword());

        assertEquals("username", oracleWithNormalLogon.getConnection().getUser());
        assertEquals("password", oracleWithNormalLogon.getConnection().getPassword());
        assertNull(oracleWithNormalLogon.getConnection().getOracleLogonAs());

        assertEquals("system", oracleWithSysdbaLogon.getConnection().getUser());
        assertEquals("passwordaqui", oracleWithSysdbaLogon.getConnection().getPassword());
        assertEquals("sysdba", oracleWithSysdbaLogon.getConnection().getOracleLogonAs());
    }
}
