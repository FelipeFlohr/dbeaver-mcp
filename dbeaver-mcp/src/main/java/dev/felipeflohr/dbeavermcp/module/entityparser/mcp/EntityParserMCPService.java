package dev.felipeflohr.dbeavermcp.module.entityparser.mcp;

import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.ParsedEntityRecord;
import dev.felipeflohr.dbeavermcp.module.entityparser.service.EntityParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@SuppressWarnings("unused")
@NullMarked
@RequiredArgsConstructor
@Service
public class EntityParserMCPService {
    public static final String GET_TABLE_COLUMNS_FROM_JPA_ENTITY_TOOL_NAME = "get_table_columns_from_jpa_entity";
    private final EntityParserService entityParserService;

    @McpTool(
            name = GET_TABLE_COLUMNS_FROM_JPA_ENTITY_TOOL_NAME,
            description = "Parses a Java JPA entity source file and extracts its database table name and column mappings. " +
                    "This tool ONLY works with Java projects using JPA/Hibernate annotations. " +
                    "Provide the absolute file path to a .java file containing a class annotated with @Entity. " +
                    "The tool extracts the database table name and all column mappings, including " +
                    "@Column, @JoinColumn, @JoinColumns, @Embedded, @EmbeddedId, and inherited @MappedSuperclass fields. " +
                    "Use the namingStrategy parameter to match the project's naming convention " +
                    "(e.g., TO_SNAKE_CASE for Hibernate's default implicit naming strategy)."
    )
    public @Nullable ParsedEntityRecord getTableColumnsFromJpaEntity(
            @McpToolParam(description = "Absolute file path to the Java JPA entity source file (e.g., /path/to/MyEntity.java)")
            String filePath,

            @McpToolParam(description = "Optional: specific class name to parse if the file contains multiple classes. Leave empty to parse the first entity found.")
            @Nullable String className,

            @McpToolParam(description = "Naming strategy for implicit column/table names: TO_SNAKE_CASE (Hibernate default), TO_UPPER_CASE, TO_LOWER_CASE, or DO_NOTHING")
            String namingStrategy,

            @McpToolParam(description = "Optional: absolute path to the Java source root directory (e.g., /path/to/src/main/java). " +
                    "Required for resolving @Embedded types and @MappedSuperclass inheritance across files. " +
                    "If not provided, the tool will attempt to auto-detect it from the file path.")
            @Nullable String sourceRoot
    ) {
        try {
            NamingStrategy strategy = NamingStrategy.valueOf(namingStrategy);
            return entityParserService.getEntityByFilePath(filePath, className, strategy, sourceRoot);
        } catch (IllegalArgumentException e) {
            log.error("Invalid naming strategy: {}", namingStrategy, e);
            throw new IllegalArgumentException("Invalid naming strategy '%s'. Valid values: TO_SNAKE_CASE, TO_UPPER_CASE, TO_LOWER_CASE, DO_NOTHING".formatted(namingStrategy));
        }
    }
}
