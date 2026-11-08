package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Layer 1 Problem Statement Validator.
 * Orchestrates a sequential chain of ProblemValidationRule instances across both online and offline modes.
 */
@Service
public class ProblemStatementValidator {

    private final List<ProblemValidationRule> rules;

    public ProblemStatementValidator(List<ProblemValidationRule> rules) {
        this.rules = (rules != null && !rules.isEmpty())
                ? rules
                : List.of(
                        new MinLengthAndWordCountRule(),
                        new LetterRatioRule(),
                        new GibberishMashRule(),
                        new RecognizableWordsRule(),
                        new TaskSignalWordRule()
                );
    }

    /**
     * Executes the validation chain on the raw problem description.
     * The first failing rule throws an InvalidProblemStatementException.
     *
     * @param rawDescription raw text entered by the user
     * @throws InvalidProblemStatementException if any validation rule fails
     */
    public void validate(String rawDescription) throws InvalidProblemStatementException {
        if (rawDescription == null || rawDescription.trim().isEmpty()) {
            throw new InvalidProblemStatementException(
                "Problem description cannot be empty.",
                List.of(
                    "Please provide a problem statement describing the algorithmic task.",
                    "Include inputs, expected outputs, and any known constraints."
                )
            );
        }

        for (ProblemValidationRule rule : rules) {
            rule.validate(rawDescription);
        }
    }

    public List<ProblemValidationRule> getRules() {
        return rules;
    }
}
