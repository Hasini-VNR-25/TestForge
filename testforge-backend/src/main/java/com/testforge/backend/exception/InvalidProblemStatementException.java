package com.testforge.backend.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a problem statement fails semantic or structural validation checks.
 * Carries an explanatory reason and actionable suggestions for the user.
 */
public class InvalidProblemStatementException extends TestForgeException {

    private final String reason;
    private final List<String> suggestions;

    public InvalidProblemStatementException(String reason, List<String> suggestions) {
        super(reason);
        this.reason = reason != null ? reason : "Invalid problem statement.";
        this.suggestions = suggestions != null ? Collections.unmodifiableList(suggestions) : Collections.emptyList();
    }

    public InvalidProblemStatementException(String reason) {
        this(reason, Collections.emptyList());
    }

    public String getReason() {
        return reason;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }
}
