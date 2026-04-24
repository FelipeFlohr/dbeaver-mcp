package dev.felipeflohr.dbeavermcp.module.entityparser.controller;

import dev.felipeflohr.dbeavermcp.module.entityparser.model.EntityAttributeRecord;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.ParsedEntityRecord;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "spring.ai.mcp.server.stdio=false")
class EntityParserControllerE2ETest {
    private static final String PACKAGE_NAME = "com.example.entity";

    @TempDir
    Path tempDir;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnParsedEntityWhenValidFilePathIsProvided() throws IOException {
        String source = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = "users")
                public class UserEntity {
                    @Id
                    private Long id;
                    private String firstName;
                    private String email;
                }
                """;
        Path file = writeJavaFile("UserEntity.java", source);

        ResponseEntity<String> response = restTemplate.getForEntity(url(file, "TO_SNAKE_CASE"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Body: " + response.getBody());
        assertNotNull(response.getBody(), "Expected non-null body for valid entity");
        ParsedEntityRecord parsed = objectMapper.readValue(response.getBody(), ParsedEntityRecord.class);
        assertNotNull(parsed);
        assertEquals("UserEntity", parsed.className());
        assertEquals("users", parsed.databaseName());
        assertContainsColumn(parsed.attributes(), "first_name");
        assertContainsColumn(parsed.attributes(), "email");
    }

    @Test
    void shouldReturnBadRequestWhenNamingStrategyIsInvalid() throws IOException {
        Path file = writeJavaFile("AnyEntity.java", "public class AnyEntity {}");
        ResponseEntity<String> response = restTemplate.getForEntity(url(file, "FOO_BAR"), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWithEmptyBodyWhenClassIsNotAnEntity() throws IOException {
        String source = """
                public class PlainClass {
                    private Long id;
                }
                """;
        Path file = writeJavaFile("PlainClass.java", source);

        ResponseEntity<String> response = restTemplate.getForEntity(url(file, "TO_SNAKE_CASE"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() == null || response.getBody().isBlank(),
                "Expected empty body for non-entity class, got: " + response.getBody());
    }

    private URI url(Path file, String namingStrategy) {
        return UriComponentsBuilder.fromPath("/entity-parser/file-path")
                .queryParam("filePath", file.toString())
                .queryParam("namingStrategy", namingStrategy)
                .queryParam("sourceRoot", tempDir.toString())
                .build()
                .encode()
                .toUri();
    }

    private Path writeJavaFile(String fileName, String content) throws IOException {
        Path packageDir = tempDir;
        for (String part : PACKAGE_NAME.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);
        Path file = packageDir.resolve(fileName);
        Files.writeString(file, "package " + PACKAGE_NAME + ";\n\n" + content);
        return file;
    }

    private void assertContainsColumn(Set<EntityAttributeRecord> attributes, String columnName) {
        boolean found = attributes.stream().anyMatch(a -> columnName.equals(a.tableName()));
        assertTrue(found, "Expected column [%s] not found in: %s".formatted(columnName, attributes));
    }
}
