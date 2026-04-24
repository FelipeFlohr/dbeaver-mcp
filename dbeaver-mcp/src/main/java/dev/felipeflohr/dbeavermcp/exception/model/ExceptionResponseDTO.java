package dev.felipeflohr.dbeavermcp.exception.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionResponseDTO {
    private String message;
    private String stackTrace;
}
