package dev.felipeflohr.dbeavermcp.module.entityparser.model;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public record ParsedEntityRecord(
        String className,
        String filePath,
        String databaseName,
        Set<EntityAttributeRecord> attributes
) {}
