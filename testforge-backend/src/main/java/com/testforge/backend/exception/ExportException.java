package com.testforge.backend.exception;

/**
 * Thrown when an export strategy encounters an I/O or serialization failure.
 */
public class ExportException extends TestForgeException {

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
