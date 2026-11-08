package com.testforge.backend.exception;

/**
 * Thrown when AI or offline heuristic test generation fails.
 */
public class GenerationFailedException extends TestForgeException {

    public GenerationFailedException(String message) {
        super(message);
    }

    public GenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
