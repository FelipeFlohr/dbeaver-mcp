package dev.felipeflohr.dbeavermcp.exception;

public class DBeaverMCPValidationException extends Exception {
    public DBeaverMCPValidationException(String message) {
        super(message);
    }

    public DBeaverMCPValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
