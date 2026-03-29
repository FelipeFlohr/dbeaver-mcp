package dev.felipeflohr.dbeavermcp.module.entityparser.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record EntityAttributeRecord(
        String attributeName,
        String tableName
) {}
