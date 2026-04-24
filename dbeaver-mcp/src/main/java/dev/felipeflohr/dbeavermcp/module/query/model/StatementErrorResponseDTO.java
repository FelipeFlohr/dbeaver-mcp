package dev.felipeflohr.dbeavermcp.module.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementErrorResponseDTO {
    private String message;
    private String stackTrace;
    private @Nullable String hint;
}
