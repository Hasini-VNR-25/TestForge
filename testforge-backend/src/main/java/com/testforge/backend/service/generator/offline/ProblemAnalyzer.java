package com.testforge.backend.service.generator.offline;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B1: ProblemAnalyzer.
 * Extracts input types, operations, numeric constraints, and examples using word-boundary regex.
 * Computes a confidence score (0.0 - 1.0).
 */
@Component
public class ProblemAnalyzer {

    // Input types using word boundaries (avoids substring false positives)
    private static final Pattern PATTERN_MATRIX = Pattern.compile("\\b(matrix|matrices|2d\\s+arrays?|grid)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_ARRAY = Pattern.compile("\\b(arrays?|lists?|nums|vectors?|elements?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_STRING = Pattern.compile("\\b(strings?|words?|chars?|characters?|substrings?|text)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_INTEGER = Pattern.compile("\\b(integers?|numbers?|digits?|values?|scalar)\\b", Pattern.CASE_INSENSITIVE);

    // Operations using word boundaries (e.g. \b(sums?)\b never matches "assume")
    private static final Pattern OP_REVERSE = Pattern.compile("\\b(reverse|reversed|reversing|invert)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_SUM = Pattern.compile("\\b(sum|sums|summation|add|addition)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_SORT = Pattern.compile("\\b(sort|sorted|sorting|ascending|descending)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_PALINDROME = Pattern.compile("\\b(palindrome|palindromes|palindromic)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_PRIME = Pattern.compile("\\b(prime|primes)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_FACTORIAL = Pattern.compile("\\b(factorial|factorials)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_FIBONACCI = Pattern.compile("\\b(fibonacci)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_MAX = Pattern.compile("\\b(maximum|greatest|largest|max)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OP_MIN = Pattern.compile("\\b(minimum|smallest|least|min)\\b", Pattern.CASE_INSENSITIVE);

    // Numeric constraints:
    // e.g., 1 <= n <= 100, 1 ≤ n ≤ 10^5, 1 <= s.length <= 100
    private static final Pattern DOUBLE_INEQUALITY = Pattern.compile(
            "(-?\\d+(?:\\^\\d+)?)\\s*(?:<=|≤|<)\\s*([a-zA-Z]+(?:\\.[a-zA-Z]+)?)\\s*(?:<=|≤|<)\\s*(-?\\d+(?:\\^\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    // e.g., n <= 100 or s.length <= 100
    private static final Pattern SINGLE_MAX_INEQUALITY = Pattern.compile(
            "\\b([a-zA-Z]+(?:\\.[a-zA-Z]+)?)\\s*(?:<=|≤)\\s*(-?\\d+(?:\\^\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    // e.g., n >= 1
    private static final Pattern SINGLE_MIN_INEQUALITY = Pattern.compile(
            "\\b([a-zA-Z]+(?:\\.[a-zA-Z]+)?)\\s*(?:>=|≥)\\s*(-?\\d+(?:\\^\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    // Task signals
    private static final Pattern TASK_SIGNALS = Pattern.compile(
            "\\b(given|find|return|compute|calculate|determine|solve|implement|write|check)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Examples: Input: ... Output: ...
    private static final Pattern EXAMPLE_PATTERN = Pattern.compile(
            "(?i)input:\\s*([^\\r\\n;]+)[\\r\\n\\s]+output:\\s*([^\\r\\n;]+)"
    );

    public AnalysisResult analyze(String statement) {
        if (statement == null || statement.isBlank()) {
            return new AnalysisResult("", "unknown", "unknown", false, null, null, null, List.of(), 0.0);
        }

        // 1. Detect Input Type
        String inputType = "unknown";
        if (PATTERN_MATRIX.matcher(statement).find()) {
            inputType = "matrix";
        } else if (PATTERN_ARRAY.matcher(statement).find()) {
            inputType = "array";
        } else if (PATTERN_STRING.matcher(statement).find()) {
            inputType = "string";
        } else if (PATTERN_INTEGER.matcher(statement).find()) {
            inputType = "integer";
        }

        // 2. Detect Operation
        String operation = "unknown";
        if (OP_REVERSE.matcher(statement).find()) {
            operation = "reverse";
        } else if (OP_PALINDROME.matcher(statement).find()) {
            operation = "palindrome";
        } else if (OP_SUM.matcher(statement).find()) {
            operation = "sum";
        } else if (OP_SORT.matcher(statement).find()) {
            operation = "sort";
        } else if (OP_FACTORIAL.matcher(statement).find()) {
            operation = "factorial";
        } else if (OP_PRIME.matcher(statement).find()) {
            operation = "prime";
        } else if (OP_FIBONACCI.matcher(statement).find()) {
            operation = "fibonacci";
        } else if (OP_MAX.matcher(statement).find()) {
            operation = "max";
        } else if (OP_MIN.matcher(statement).find()) {
            operation = "min";
        }

        // 3. Extract Constraints
        boolean hasConstraints = false;
        Long minConstraint = null;
        Long maxConstraint = null;
        String constraintVar = null;

        Matcher dblMatcher = DOUBLE_INEQUALITY.matcher(statement);
        if (dblMatcher.find()) {
            hasConstraints = true;
            minConstraint = parseConstraintNumber(dblMatcher.group(1));
            constraintVar = dblMatcher.group(2);
            maxConstraint = parseConstraintNumber(dblMatcher.group(3));
        } else {
            Matcher maxMatcher = SINGLE_MAX_INEQUALITY.matcher(statement);
            if (maxMatcher.find()) {
                hasConstraints = true;
                constraintVar = maxMatcher.group(1);
                maxConstraint = parseConstraintNumber(maxMatcher.group(2));
            }
            Matcher minMatcher = SINGLE_MIN_INEQUALITY.matcher(statement);
            if (minMatcher.find()) {
                hasConstraints = true;
                if (constraintVar == null) constraintVar = minMatcher.group(1);
                minConstraint = parseConstraintNumber(minMatcher.group(2));
            }
        }

        // 4. Extract Examples
        List<AnalysisResult.ExampleCase> examples = new ArrayList<>();
        Matcher exMatcher = EXAMPLE_PATTERN.matcher(statement);
        while (exMatcher.find()) {
            String in = exMatcher.group(1).trim();
            String out = exMatcher.group(2).trim();
            if (!in.isEmpty() && !out.isEmpty()) {
                examples.add(new AnalysisResult.ExampleCase(in, out));
            }
        }

        // 5. Compute Confidence Score
        double confidence = 0.0;
        if (!"unknown".equals(inputType)) confidence += 0.35;
        if (!"unknown".equals(operation)) confidence += 0.35;
        if (hasConstraints) confidence += 0.15;
        if (!examples.isEmpty()) confidence += 0.15;
        if (TASK_SIGNALS.matcher(statement).find()) confidence += 0.10;

        // Cap confidence between 0.0 and 1.0
        confidence = Math.min(1.0, confidence);

        return new AnalysisResult(
                statement,
                inputType,
                operation,
                hasConstraints,
                minConstraint,
                maxConstraint,
                constraintVar,
                examples,
                confidence
        );
    }

    private Long parseConstraintNumber(String raw) {
        if (raw == null) return null;
        String clean = raw.trim().replace(" ", "");
        try {
            if (clean.contains("^")) {
                String[] parts = clean.split("\\^");
                long base = Long.parseLong(parts[0]);
                long exp = Long.parseLong(parts[1]);
                return (long) Math.pow(base, exp);
            }
            return Long.parseLong(clean);
        } catch (Exception e) {
            return null;
        }
    }
}
