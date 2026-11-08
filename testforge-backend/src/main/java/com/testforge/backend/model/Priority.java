package com.testforge.backend.model;

/**
 * Type-safe enumeration for test case execution priority.
 * Implements natural ordering where HIGH has the highest precedence.
 */
public enum Priority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static boolean isValid(String priorityName) {
        if (priorityName == null || priorityName.isBlank()) return false;
        try {
            Priority.valueOf(priorityName.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Priority from(String priorityName) {
        if (priorityName == null || priorityName.isBlank()) return MEDIUM;
        try {
            return Priority.valueOf(priorityName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
