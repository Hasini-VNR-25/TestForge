package com.testforge.backend.exception;

/**
 * Thrown when test case validation invariants are violated
 * (e.g., blank input, missing expected output, invalid category).
 */
public class InvalidTestCaseException extends TestForgeException {

    public InvalidTestCaseException(String message) {
        super(message);
    }

    public InvalidTestCaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
