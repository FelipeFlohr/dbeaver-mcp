package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverConnectionDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources.DBeaverDataSourcesDTO;
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


import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class DBeaverDataSourceServiceTest {
    @Autowired
    private DBeaverDataSourceService service;

    @MockitoBean
    private DBeaverFilesService filesService;

    @Value("classpath:data-sources.json")
    private Resource dataSource;

    @BeforeEach
    void beforeEach() throws IOException {
        MockDBeaverFilesUtil.mockFilesDataSourcesFilePath(filesService, dataSource.getFilePath().toAbsolutePath().toString());
    }

    @Test
    void getDataSource() throws DBeaverMCPValidationException {
        DBeaverDataSourcesDTO dataSources = service.getDataSources();
        assertEquals(4, dataSources.getConnections().size());

        DBeaverConnectionDTO localPostgresConnection = dataSources.getConnections().get("postgres-jdbc-196079609f2-53d190edd595caaa");
        assertNotNull(localPostgresConnection);
        assertEquals("postgresql", localPostgresConnection.getProvider());
        assertEquals("postgres-jdbc", localPostgresConnection.getDriver());
        assertEquals("Local Postgres", localPostgresConnection.getName());
        assertNotNull(localPostgresConnection.getConfiguration());
        assertEquals("jdbc:postgresql://localhost:5432/postgres", localPostgresConnection.getConfiguration().getUrl());
        assertNull(localPostgresConnection.getConfiguration().getHandlers());

        DBeaverConnectionDTO nvrServerConnection = dataSources.getConnections().get("postgres-jdbc-19bc99acaa1-78b516adfdeb47d9");
        assertNotNull(nvrServerConnection);
        assertEquals("postgresql", nvrServerConnection.getProvider());
        assertEquals("postgres-jdbc", nvrServerConnection.getDriver());
        assertEquals("NVR - Server", nvrServerConnection.getName());
        assertNotNull(nvrServerConnection.getConfiguration());
        assertEquals("jdbc:postgresql://localhost:5432/nvr", nvrServerConnection.getConfiguration().getUrl());
        assertNotNull(nvrServerConnection.getConfiguration().getHandlers());
        assertNotNull(nvrServerConnection.getConfiguration().getHandlers().getSshTunnel());
        assertTrue(nvrServerConnection.getConfiguration().getHandlers().getSshTunnel().isEnabled());
        assertEquals("192.168.1.20", nvrServerConnection.getConfiguration().getHandlers().getSshTunnel().getProperties().getHost());
        assertEquals(22, nvrServerConnection.getConfiguration().getHandlers().getSshTunnel().getProperties().getPort());
        assertEquals("PASSWORD", nvrServerConnection.getConfiguration().getHandlers().getSshTunnel().getProperties().getAuthType());

        DBeaverConnectionDTO oracleNormal = dataSources.getConnections().get("oracle_thin-19c2146e52e-70ea308a7473e0a9");
        assertNotNull(oracleNormal);
        assertEquals("oracle", oracleNormal.getProvider());
        assertEquals("oracle_thin", oracleNormal.getDriver());
        assertEquals("Oracle Normal", oracleNormal.getName());
        assertNotNull(oracleNormal.getConfiguration());
        assertEquals("jdbc:oracle:thin:@//localhost:1521/ORCL", oracleNormal.getConfiguration().getUrl());
        assertNotNull(oracleNormal.getConfiguration().getHandlers());
        assertNull(oracleNormal.getConfiguration().getHandlers().getSshTunnel());

        DBeaverConnectionDTO oracleAsSysdba = dataSources.getConnections().get("oracle_thin-19c2147fa39-6d55b3e69d72ab41");
        assertNotNull(oracleAsSysdba);
        assertEquals("oracle", oracleAsSysdba.getProvider());
        assertEquals("oracle_thin", oracleAsSysdba.getDriver());
        assertEquals("Oracle as SYSDBA", oracleAsSysdba.getName());
        assertNotNull(oracleAsSysdba.getConfiguration());
        assertEquals("jdbc:oracle:thin:@localhost:1521:ORCLSIDASSYSDBA", oracleAsSysdba.getConfiguration().getUrl());
        assertNull(oracleAsSysdba.getConfiguration().getHandlers());
    }
}
