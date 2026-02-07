package dev.felipeflohr.dbeavermcp.module.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseObjectWithCodeDTO {
    private String name;
    private String sourceCode;
}
