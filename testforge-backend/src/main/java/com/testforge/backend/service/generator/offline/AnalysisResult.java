package com.testforge.backend.service.generator.offline;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates problem statement static analysis findings.
 */
public class AnalysisResult {

    public record ExampleCase(String input, String output) {}

    private final String rawStatement;
    private final String inputType; // "array", "string", "integer", "matrix", or "unknown"
    private final String operation; // "reverse", "sum", "sort", "palindrome", "prime", "factorial", "fibonacci", or "unknown"
    private final boolean hasConstraints;
    private final Long minConstraint;
    private final Long maxConstraint;
    private final String constraintVar;
    private final List<ExampleCase> examples;
    private final double confidenceScore;

    public AnalysisResult(
            String rawStatement,
            String inputType,
            String operation,
            boolean hasConstraints,
            Long minConstraint,
            Long maxConstraint,
            String constraintVar,
            List<ExampleCase> examples,
            double confidenceScore
    ) {
        this.rawStatement = rawStatement;
        this.inputType = inputType != null ? inputType : "unknown";
        this.operation = operation != null ? operation : "unknown";
        this.hasConstraints = hasConstraints;
        this.minConstraint = minConstraint;
        this.maxConstraint = maxConstraint;
        this.constraintVar = constraintVar;
        this.examples = examples != null ? Collections.unmodifiableList(examples) : Collections.emptyList();
        this.confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore));
    }

    public String getRawStatement() {
        return rawStatement;
    }

    public String getInputType() {
        return inputType;
    }

    public String getOperation() {
        return operation;
    }

    public boolean hasConstraints() {
        return hasConstraints;
    }

    public Long getMinConstraint() {
        return minConstraint;
    }

    public Long getMaxConstraint() {
        return maxConstraint;
    }

    public String getConstraintVar() {
        return constraintVar;
    }

    public List<ExampleCase> getExamples() {
        return examples;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public boolean isOperationRecognized() {
        return !"unknown".equalsIgnoreCase(operation);
    }

    public boolean isInputTypeRecognized() {
        return !"unknown".equalsIgnoreCase(inputType);
    }
}
