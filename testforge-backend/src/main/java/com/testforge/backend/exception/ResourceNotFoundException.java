package com.testforge.backend.exception;

/**
 * Thrown when a requested Problem or TestCase entity cannot be located.
 */
public class ResourceNotFoundException extends TestForgeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
