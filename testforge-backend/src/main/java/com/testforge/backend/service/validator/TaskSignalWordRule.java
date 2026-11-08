package com.testforge.backend.service.validator;

import com.testforge.backend.exception.InvalidProblemStatementException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule 5: Requires at least one algorithmic task signal word (e.g., given, find, return, compute, input, output).
 */
public class TaskSignalWordRule extends ProblemValidationRule {

    private static final Pattern SIGNAL_WORD_PATTERN = Pattern.compile(
        "\\b(given|find|return|returns|returned|compute|computes|computed|calculate|calculates|calculated|" +
        "determine|determines|determined|check|checks|checked|implement|implements|implemented|write|writes|wrote|" +
        "solve|solves|solved|reverse|reversed|reversing|sort|sorted|sorting|count|counts|counted|generate|generates|" +
        "input|inputs|output|outputs|evaluate|evaluates|print|prints|produce|produces|identify|identifies|" +
        "construct|constructs|transform|transforms|convert|converts|process|processes|search|searches)\\b",
        Pattern.CASE_INSENSITIVE
    );

    public TaskSignalWordRule() {
        super("TaskSignalWordRule");
    }

    @Override
    public void validate(String rawDescription) throws InvalidProblemStatementException {
        if (rawDescription == null || rawDescription.isBlank()) return;

        if (!SIGNAL_WORD_PATTERN.matcher(rawDescription).find()) {
            throw new InvalidProblemStatementException(
                "Problem description lacks an explicit task signal word (such as 'given', 'find', 'return', 'compute', 'input', or 'output').",
                List.of(
                    "Include task signal words to specify what is given and what needs to be found or returned.",
                    "Example: 'Given a string, return it reversed.' or 'Compute the sum of elements in an array.'"
                )
            );
        }
    }
}
