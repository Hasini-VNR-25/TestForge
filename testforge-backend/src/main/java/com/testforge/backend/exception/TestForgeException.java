package com.testforge.backend.exception;

/**
 * Base unchecked exception for all TestForge application domain errors.
 */
public class TestForgeException extends RuntimeException {

    public TestForgeException(String message) {
        super(message);
    }

    public TestForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
