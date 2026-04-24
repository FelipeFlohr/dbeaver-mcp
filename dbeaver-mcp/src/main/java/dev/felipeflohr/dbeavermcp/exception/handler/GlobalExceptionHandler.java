package dev.felipeflohr.dbeavermcp.exception.handler;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.exception.model.ExceptionResponseDTO;
import dev.felipeflohr.dbeavermcp.util.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@NullMarked
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DBeaverMCPValidationException.class)
    public ResponseEntity<ExceptionResponseDTO> handleValidationException(DBeaverMCPValidationException e) {
        ExceptionResponseDTO body = new ExceptionResponseDTO(
                e.getMessage(),
                StringUtils.getExceptionStackTraceAsString(e)
        );
        return ResponseEntity.badRequest().body(body);
    }
}
