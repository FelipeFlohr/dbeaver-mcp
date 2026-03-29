package dev.felipeflohr.dbeavermcp.module.entityparser.service;

import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.ParsedEntityRecord;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface EntityParserService {
    @Nullable
    ParsedEntityRecord getEntityByFilePath(String filePath, @Nullable String className, NamingStrategy namingStrategy, @Nullable String sourceRoot);
}
