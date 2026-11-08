package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;
import java.util.List;

/**
 * Rule 1: Enforces minimum length and minimum word count invariants.
 */
public class MinLengthAndWordCountRule extends ProblemValidationRule {

    private static final int MIN_LENGTH = 15;
    private static final int MIN_WORD_COUNT = 3;

    public MinLengthAndWordCountRule() {
        super("MinLengthAndWordCountRule");
    }

    @Override
    public void validate(String rawDescription) throws InvalidProblemStatementException {
        if (rawDescription == null || rawDescription.trim().isEmpty()) {
            throw new InvalidProblemStatementException(
                "Problem description is empty.",
                List.of(
                    "Enter a complete problem statement describing inputs, expected outputs, and constraints.",
                    "Ensure the statement contains at least " + MIN_WORD_COUNT + " words."
                )
            );
        }

        String trimmed = rawDescription.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new InvalidProblemStatementException(
                "Problem description is too short (" + trimmed.length() + " characters). Minimum required is " + MIN_LENGTH + " characters.",
                List.of(
                    "Describe the task in more detail.",
                    "Include what inputs are provided and what output should be computed or returned."
                )
            );
        }

        String[] words = trimmed.split("\\s+");
        if (words.length < MIN_WORD_COUNT) {
            throw new InvalidProblemStatementException(
                "Problem description contains only " + words.length + " word(s). Minimum required is " + MIN_WORD_COUNT + " words.",
                List.of(
                    "Provide a full sentence explaining the problem.",
                    "Describe the requirements and behavior of the expected algorithm."
                )
            );
        }
    }
}
