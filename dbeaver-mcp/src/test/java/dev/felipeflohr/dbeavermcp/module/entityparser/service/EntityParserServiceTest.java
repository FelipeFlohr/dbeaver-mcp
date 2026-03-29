package dev.felipeflohr.dbeavermcp.module.entityparser.service;

import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.EntityAttributeRecord;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.ParsedEntityRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EntityParserServiceTest {
    private static final String PACKAGE_NAME = "com.example.entity";

    @TempDir
    Path tempDir;

    private EntityParserServiceImpl service;
    private Path sourceRoot;

    @BeforeEach
    void setUp() {
        service = new EntityParserServiceImpl();
        sourceRoot = tempDir;
    }

    @Test
    void basicEntity_snakeCase() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;

                @Entity
                public class BasicEntity {
                    @Id
                    private Long id;
                    private String firstName;
                    private int orderCount;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "BasicEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertEquals("BasicEntity", result.className());
        assertEquals("basic_entity", result.databaseName());
        assertContainsColumn(result.attributes(), "id", "id");
        assertContainsColumn(result.attributes(), "firstName", "first_name");
        assertContainsColumn(result.attributes(), "orderCount", "order_count");
        assertEquals(3, result.attributes().size());
    }

    @Test
    void columnAnnotation_explicitName() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Column;

                @Entity
                public class ExplicitColumnEntity {
                    @Id
                    private Long id;

                    @Column(name = "CUSTOM_COL")
                    private String myField;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "ExplicitColumnEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "myField", "CUSTOM_COL");
    }

    @Test
    void columnAnnotation_noNameAttribute() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Column;

                @Entity
                public class ColumnNoNameEntity {
                    @Id
                    private Long id;

                    @Column(nullable = false)
                    private String myField;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "ColumnNoNameEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "myField", "my_field");
    }

    @Test
    void joinColumn_explicitName() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.ManyToOne;
                import jakarta.persistence.JoinColumn;

                @Entity
                public class JoinColumnEntity {
                    @Id
                    private Long id;

                    @ManyToOne
                    @JoinColumn(name = "FK_PARENT")
                    private Object parent;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "JoinColumnEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "parent", "FK_PARENT");
    }

    @Test
    void joinColumn_implicit() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.ManyToOne;

                @Entity
                public class ImplicitJoinEntity {
                    @Id
                    private Long id;

                    @ManyToOne
                    private Object parentEntity;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "ImplicitJoinEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "parentEntity", "parent_entity_id");
    }

    @Test
    void joinColumns_multiple() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.ManyToOne;
                import jakarta.persistence.JoinColumns;
                import jakarta.persistence.JoinColumn;
                import jakarta.persistence.ForeignKey;

                @Entity
                public class JoinColumnsEntity {
                    @Id
                    private Long id;

                    @ManyToOne
                    @JoinColumns(value = {
                        @JoinColumn(name = "IDTITULO", referencedColumnName = "IDTITULO"),
                        @JoinColumn(name = "NUMERO", referencedColumnName = "NUMERO")
                    }, foreignKey = @ForeignKey(name = "FK_PARCELA_ORIGEM"))
                    private Object parcela;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "JoinColumnsEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "parcela", "IDTITULO");
        assertContainsColumn(result.attributes(), "parcela", "NUMERO");
        long parcelaColumns = result.attributes().stream()
                .filter(a -> "parcela".equals(a.attributeName()))
                .count();
        assertEquals(2, parcelaColumns);
    }

    @Test
    void embedded_withAttributeOverrides() throws IOException {
        // language=java
        var addressSource = """
                import jakarta.persistence.Embeddable;

                @Embeddable
                public class Address {
                    private String street;
                    private String city;
                    private String zipCode;
                }
                """;
        writeJavaFile(PACKAGE_NAME, "Address.java", addressSource);

        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Embedded;
                import jakarta.persistence.AttributeOverrides;
                import jakarta.persistence.AttributeOverride;
                import jakarta.persistence.Column;

                @Entity
                public class EmbeddedEntity {
                    @Id
                    private Long id;

                    @Embedded
                    @AttributeOverrides({
                        @AttributeOverride(name = "street", column = @Column(name = "HOME_STREET")),
                        @AttributeOverride(name = "city", column = @Column(name = "HOME_CITY"))
                    })
                    private Address homeAddress;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "EmbeddedEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "street", "HOME_STREET");
        assertContainsColumn(result.attributes(), "city", "HOME_CITY");
        assertContainsColumn(result.attributes(), "zipCode", "zip_code");
    }

    @Test
    void embeddedId() throws IOException {
        // language=java
        var pkSource = """
                import jakarta.persistence.Embeddable;

                @Embeddable
                public class OrderPK {
                    private Long orderId;
                    private Long productId;
                }
                """;
        writeJavaFile(PACKAGE_NAME, "OrderPK.java", pkSource);

        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.EmbeddedId;

                @Entity
                public class EmbeddedIdEntity {
                    @EmbeddedId
                    private OrderPK pk;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "EmbeddedIdEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "orderId", "order_id");
        assertContainsColumn(result.attributes(), "productId", "product_id");
    }

    @Test
    void mappedSuperclass_inheritance() throws IOException {
        // language=java
        var baseSource = """
                import jakarta.persistence.MappedSuperclass;
                import jakarta.persistence.Id;
                import jakarta.persistence.Column;

                @MappedSuperclass
                public class BaseEntity {
                    @Id
                    private Long id;

                    @Column(name = "CREATED_AT")
                    private String createdAt;
                }
                """;
        writeJavaFile(PACKAGE_NAME, "BaseEntity.java", baseSource);

        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Column;

                @Entity
                public class ChildEntity extends BaseEntity {
                    @Column(name = "CHILD_NAME")
                    private String childName;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "ChildEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertEquals("ChildEntity", result.className());
        assertContainsColumn(result.attributes(), "id", "id");
        assertContainsColumn(result.attributes(), "createdAt", "CREATED_AT");
        assertContainsColumn(result.attributes(), "childName", "CHILD_NAME");
    }

    @Test
    void skippedFields() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Transient;
                import jakarta.persistence.OneToMany;
                import jakarta.persistence.ManyToMany;
                import jakarta.persistence.ElementCollection;
                import java.util.List;
                import java.util.Set;

                @Entity
                public class SkippedFieldsEntity {
                    @Id
                    private Long id;

                    private static String CONSTANT = "value";
                    private transient String temp;

                    @Transient
                    private String computed;

                    @OneToMany
                    private List<Object> children;

                    @ManyToMany
                    private Set<Object> tags;

                    @ElementCollection
                    private List<String> nicknames;

                    private String normalField;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "SkippedFieldsEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertEquals(2, result.attributes().size());
        assertContainsColumn(result.attributes(), "id", "id");
        assertContainsColumn(result.attributes(), "normalField", "normal_field");
    }

    @Test
    void idAndGeneratedValue() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.GeneratedValue;
                import jakarta.persistence.GenerationType;
                import jakarta.persistence.Column;

                @Entity
                public class GeneratedIdEntity {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    @Column(name = "ENTITY_ID")
                    private Long id;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "GeneratedIdEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "id", "ENTITY_ID");
    }

    @Test
    void enumeratedField() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Enumerated;
                import jakarta.persistence.EnumType;

                @Entity
                public class EnumEntity {
                    @Id
                    private Long id;

                    @Enumerated(EnumType.STRING)
                    private String status;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "EnumEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "status", "status");
    }

    @Test
    void versionField() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Version;

                @Entity
                public class VersionEntity {
                    @Id
                    private Long id;

                    @Version
                    private Long version;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "VersionEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "version", "version");
    }

    @Test
    void tableAnnotation_explicitName() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = "MY_CUSTOM_TABLE")
                public class TableNameEntity {
                    @Id
                    private Long id;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "TableNameEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertEquals("MY_CUSTOM_TABLE", result.databaseName());
    }

    @Test
    void nonEntityClass_returnsNull() throws IOException {
        // language=java
        var sourceCode = """
                public class NotAnEntity {
                    private Long id;
                    private String name;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "NotAnEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNull(result);
    }

    @Test
    void namingStrategy_doNothing() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;

                @Entity
                public class DoNothingEntity {
                    @Id
                    private Long id;
                    private String firstName;
                    private String lastName;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "DoNothingEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.DO_NOTHING, sourceRoot.toString());

        assertNotNull(result);
        assertEquals("DoNothingEntity", result.databaseName());
        assertContainsColumn(result.attributes(), "firstName", "firstName");
        assertContainsColumn(result.attributes(), "lastName", "lastName");
    }

    @Test
    void oneToOne_withJoinColumn() throws IOException {
        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.OneToOne;
                import jakarta.persistence.JoinColumn;

                @Entity
                public class OneToOneEntity {
                    @Id
                    private Long id;

                    @OneToOne
                    @JoinColumn(name = "PROFILE_ID")
                    private Object profile;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "OneToOneEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "profile", "PROFILE_ID");
    }

    @Test
    void singleAttributeOverride() throws IOException {
        // language=java
        var moneySource = """
                import jakarta.persistence.Embeddable;

                @Embeddable
                public class Money {
                    private double amount;
                    private String currency;
                }
                """;
        writeJavaFile(PACKAGE_NAME, "Money.java", moneySource);

        // language=java
        var sourceCode = """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Embedded;
                import jakarta.persistence.AttributeOverride;
                import jakarta.persistence.Column;

                @Entity
                public class SingleOverrideEntity {
                    @Id
                    private Long id;

                    @Embedded
                    @AttributeOverride(name = "amount", column = @Column(name = "PRICE_AMOUNT"))
                    private Money price;
                }
                """;
        Path file = writeJavaFile(PACKAGE_NAME, "SingleOverrideEntity.java", sourceCode);

        ParsedEntityRecord result = service.getEntityByFilePath(file.toString(), null, NamingStrategy.TO_SNAKE_CASE, sourceRoot.toString());

        assertNotNull(result);
        assertContainsColumn(result.attributes(), "amount", "PRICE_AMOUNT");
        assertContainsColumn(result.attributes(), "currency", "currency");
    }

    private Path writeJavaFile(String packageName, String fileName, String content) throws IOException {
        Path packageDir = sourceRoot;
        for (String part : packageName.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);
        Path file = packageDir.resolve(fileName);
        Files.writeString(file, "package " + packageName + ";\n\n" + content);
        return file;
    }

    private void assertContainsColumn(Set<EntityAttributeRecord> attributes, String attributeName, String columnName) {
        boolean found = attributes.stream()
                .anyMatch(a -> attributeName.equals(a.attributeName()) && columnName.equals(a.tableName()));
        assertTrue(found, "Expected column mapping [%s -> %s] not found in: %s".formatted(
                attributeName, columnName,
                attributes.stream()
                        .map(a -> "[%s -> %s]".formatted(a.attributeName(), a.tableName()))
                        .collect(Collectors.joining(", "))));
    }
}
