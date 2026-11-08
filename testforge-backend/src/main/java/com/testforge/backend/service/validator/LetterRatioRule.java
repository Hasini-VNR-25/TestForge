package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;
import java.util.List;

/**
 * Rule 2: Enforces minimum ratio of alphabetic letters to total non-whitespace characters.
 */
public class LetterRatioRule extends ProblemValidationRule {

    private static final double MIN_LETTER_RATIO = 0.45;

    public LetterRatioRule() {
        super("LetterRatioRule");
    }

    @Override
    public void validate(String rawDescription) throws InvalidProblemStatementException {
        if (rawDescription == null) return;

        int letterCount = 0;
        int nonWhitespaceCount = 0;

        for (int i = 0; i < rawDescription.length(); i++) {
            char c = rawDescription.charAt(i);
            if (!Character.isWhitespace(c)) {
                nonWhitespaceCount++;
                if (Character.isLetter(c)) {
                    letterCount++;
                }
            }
        }

        if (nonWhitespaceCount > 0) {
            double ratio = (double) letterCount / nonWhitespaceCount;
            if (ratio < MIN_LETTER_RATIO) {
                throw new InvalidProblemStatementException(
                    "Problem description contains insufficient alphabetic text (" + Math.round(ratio * 100) + "% letters).",
                    List.of(
                        "Explain the problem using descriptive English sentences rather than purely symbols or numbers.",
                        "Provide descriptive context around mathematical equations or constraints."
                    )
                );
            }
        }
    }
}
