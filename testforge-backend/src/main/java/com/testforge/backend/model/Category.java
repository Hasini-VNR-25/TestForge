package com.testforge.backend.model;

/**
 * Type-safe enumeration representing software test case classifications.
 * Encapsulates category descriptions and parsing invariants.
 */
public enum Category {
    NORMAL("Nominal functional verification with standard inputs"),
    BOUNDARY("Boundary value analysis at minimum and maximum threshold limits"),
    EDGE("Corner and structural edge cases (e.g. empty, single element, extreme scale)"),
    INVALID_INPUT("Negative testing with syntactically or logically erroneous inputs"),
    EXCEPTION("Scenarios designed to trigger runtime exceptions and error handling");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return false;
        try {
            Category.valueOf(categoryName.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Category from(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return NORMAL;
        try {
            return Category.valueOf(categoryName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
