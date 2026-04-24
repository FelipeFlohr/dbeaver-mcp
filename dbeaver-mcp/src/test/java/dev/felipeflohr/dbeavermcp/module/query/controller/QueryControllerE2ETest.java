package dev.felipeflohr.dbeavermcp.module.query.controller;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.exception.model.ExceptionResponseDTO;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverCipherService;
import dev.felipeflohr.dbeavermcp.module.dbeaver.service.DBeaverDataSourceService;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.test.TestcontainersConfiguration;
import dev.felipeflohr.dbeavermcp.test.TestcontainersService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "spring.ai.mcp.server.stdio=false")
class QueryControllerE2ETest {
    private static final String CONNECTION_NAME = TestcontainersConfiguration.POSTGRES_CONNECTION_NAME;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestcontainersService testcontainersService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DBeaverDataSourceService mockedDBeaverDataSourceService;

    @MockitoBean
    private DBeaverCipherService mockedDBeaverCipherService;

    @Value("classpath:scripts/parent-and-child-test-postgres.sql")
    private Resource parentAndChildTestPostgresScript;

    @Nullable
    private File asyncResultFile;

    @BeforeEach
    void beforeEach() throws DBeaverMCPValidationException, SQLException {
        testcontainersService.mockDBeaverConnections(mockedDBeaverDataSourceService, mockedDBeaverCipherService);
        testcontainersService.executePostgresScript(parentAndChildTestPostgresScript);
    }

    @AfterEach
    void afterEach() throws SQLException {
        testcontainersService.clearPostgresContainer();
        if (asyncResultFile != null && asyncResultFile.exists()) {
            assertTrue(asyncResultFile.delete(), "Failed to delete async result file: " + asyncResultFile.getAbsolutePath());
        }
    }

    @Test
    void shouldExecuteQuerySynchronously() {
        HttpEntity<List<String>> request = jsonRequest(List.of("SELECT * FROM parent_test_entity"));
        ResponseEntity<String> response = restTemplate.postForEntity(queryUrl("/query"), request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<StatementResponseDTO> statements = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(1, statements.size());
        assertEquals("SELECT * FROM parent_test_entity", statements.getFirst().getSql());
        assertNotNull(statements.getFirst().getResponse());
        assertEquals(2, statements.getFirst().getResponse().size());
    }

    @Test
    void shouldReturnBadRequestWhenStatementsIsEmpty() {
        HttpEntity<List<String>> request = jsonRequest(List.of());
        ResponseEntity<String> response = restTemplate.postForEntity(queryUrl("/query"), request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No statements", response.getBody());
    }

    @Test
    void shouldReturnBadRequestWithExceptionBodyWhenConnectionIsInvalid() {
        HttpEntity<List<String>> request = jsonRequest(List.of("SELECT 1"));
        String url = "/query?connectionName=" + URLEncoder.encode("Non existent connection", StandardCharsets.UTF_8);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ExceptionResponseDTO body = objectMapper.readValue(response.getBody(), ExceptionResponseDTO.class);
        assertNotNull(body.getMessage());
        assertTrue(body.getMessage().contains("Non existent connection"));
        assertNotNull(body.getStackTrace());
        assertTrue(body.getStackTrace().contains(DBeaverMCPValidationException.class.getName()));
    }

    @Test
    void shouldExecuteQueryAsynchronously() {
        HttpEntity<List<String>> request = jsonRequest(List.of("SELECT * FROM parent_test_entity"));
        ResponseEntity<String> response = restTemplate.postForEntity(queryUrl("/query/async"), request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String filePath = response.getBody();
        assertNotNull(filePath);
        asyncResultFile = new File(filePath);
        assertTrue(asyncResultFile.exists(), "Async result file should exist");

        await().atMost(10, SECONDS).until(() -> asyncResultFile.length() > 0);

        List<StatementResponseDTO> statements = readStatementsFromFile(asyncResultFile);
        assertEquals(1, statements.size());
        assertEquals("SELECT * FROM parent_test_entity", statements.getFirst().getSql());
        assertNotNull(statements.getFirst().getResponse());
        assertEquals(2, statements.getFirst().getResponse().size());
    }

    @Test
    void shouldReturnBadRequestWhenAsyncStatementsIsEmpty() {
        HttpEntity<List<String>> request = jsonRequest(List.of());
        ResponseEntity<String> response = restTemplate.postForEntity(queryUrl("/query/async"), request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No statements", response.getBody());
    }

    private String queryUrl(String path) {
        return path + "?connectionName=" + URLEncoder.encode(CONNECTION_NAME, StandardCharsets.UTF_8);
    }

    private HttpEntity<List<String>> jsonRequest(List<String> statements) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(statements, headers);
    }

    private List<StatementResponseDTO> readStatementsFromFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            assertFalse(content.isBlank());
            return objectMapper.readValue(content, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
