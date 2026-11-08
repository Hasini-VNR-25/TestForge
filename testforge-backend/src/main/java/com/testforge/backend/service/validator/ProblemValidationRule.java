package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;

/**
 * Abstract base class for problem statement validation rules.
 * Mirrors the modular design of AuditRule with an abstract base and specialized rule subclasses.
 */
public abstract class ProblemValidationRule {

    protected final String ruleName;

    public ProblemValidationRule(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleName() {
        return ruleName;
    }

    /**
     * Evaluates the raw problem description.
     * Throws InvalidProblemStatementException on the first violation.
     *
     * @param rawDescription raw text entered by the user
     * @throws InvalidProblemStatementException if validation fails
     */
    public abstract void validate(String rawDescription) throws InvalidProblemStatementException;
}
