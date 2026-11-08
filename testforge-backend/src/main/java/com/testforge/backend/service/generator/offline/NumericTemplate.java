package com.testforge.backend.service.generator.offline;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * B2: Template implementation for numeric and arithmetic problems.
 */
@Component
public class NumericTemplate implements ProblemTemplate {

    @Override
    public String getName() {
        return "NumericTemplate";
    }

    @Override
    public double matchScore(AnalysisResult analysis) {
        String type = analysis.getInputType();
        if ("integer".equalsIgnoreCase(type)) {
            return 1.0;
        }
        // If operation is mathematical (factorial, prime, fibonacci) and inputType is unknown
        String op = analysis.getOperation();
        if ("factorial".equalsIgnoreCase(op) || "prime".equalsIgnoreCase(op) || "fibonacci".equalsIgnoreCase(op)) {
            return 0.8;
        }
        return 0.0;
    }

    @Override
    public List<Map<String, Object>> generateTestCases(
            AnalysisResult analysis,
            int testCount,
            List<String> categories,
            ReferenceSolverRegistry solverRegistry
    ) {
        Set<String> allowedCats = (categories != null && !categories.isEmpty())
                ? new HashSet<>(categories)
                : Set.of("NORMAL", "BOUNDARY", "EDGE", "INVALID_INPUT", "EXCEPTION");

        List<Map<String, Object>> pool = new ArrayList<>();
        String op = analysis.getOperation();
        boolean canSolve = solverRegistry.canSolve("integer", op);

        // 1. Examples from problem statement
        if (allowedCats.contains("NORMAL")) {
            for (AnalysisResult.ExampleCase ex : analysis.getExamples()) {
                String input = ex.input();
                String output = canSolve ? solverRegistry.solve("integer", op, input) : ex.output();
                pool.add(createCase(input, output, "NORMAL", "HIGH", "Example from statement", "Direct problem example verification", "REQ-1", "REQ-1: Problem Example", canSolve));
            }
        }

        // 2. Constraint-driven boundary values (B3)
        if (allowedCats.contains("BOUNDARY")) {
            if (analysis.hasConstraints() && analysis.getMinConstraint() != null && analysis.getMaxConstraint() != null) {
                long min = analysis.getMinConstraint();
                long max = analysis.getMaxConstraint();
                long belowMin = min - 1;
                long aboveMax = max + 1;

                String minStr = String.valueOf(min);
                String minOut = solverRegistry.solve("integer", op, minStr);
                pool.add(createCase(minStr, minOut, "BOUNDARY", "HIGH", "Exact minimum constraint threshold (" + min + ")", "Validates boundary condition at exact minimum threshold.", "REQ-2", "REQ-2: Min Boundary", canSolve));

                String maxStr = String.valueOf(max);
                String maxOut = solverRegistry.solve("integer", op, maxStr);
                pool.add(createCase(maxStr, maxOut, "BOUNDARY", "HIGH", "Exact maximum constraint threshold (" + max + ")", "Validates boundary condition at exact maximum threshold.", "REQ-2", "REQ-2: Max Boundary", canSolve));

                String belowStr = String.valueOf(belowMin);
                String belowOut = solverRegistry.solve("integer", op, belowStr);
                pool.add(createCase(belowStr, belowOut, "BOUNDARY", "MEDIUM", "One below minimum constraint (" + belowMin + ")", "Validates boundary condition one unit below allowable minimum.", "REQ-2", "REQ-2: Below Min Boundary", canSolve));

                String aboveStr = String.valueOf(aboveMax);
                String aboveOut = solverRegistry.solve("integer", op, aboveStr);
                pool.add(createCase(aboveStr, aboveOut, "BOUNDARY", "MEDIUM", "One above maximum constraint (" + aboveMax + ")", "Validates boundary condition one unit above allowable maximum.", "REQ-2", "REQ-2: Above Max Boundary", canSolve));
            } else {
                // Fall back to generic boundary cases for integer
                String zeroStr = "0";
                pool.add(createCase(zeroStr, solverRegistry.solve("integer", op, zeroStr), "BOUNDARY", "HIGH", "Zero threshold", "Lower boundary threshold at integer zero.", "REQ-2", "REQ-2: Zero Boundary", canSolve));

                String oneStr = "1";
                pool.add(createCase(oneStr, solverRegistry.solve("integer", op, oneStr), "BOUNDARY", "MEDIUM", "Unit boundary threshold", "Unit threshold verification at integer 1.", "REQ-2", "REQ-2: Unit Boundary", canSolve));
            }
        }

        // 3. NORMAL Cases
        if (allowedCats.contains("NORMAL")) {
            String nominal1 = "10";
            pool.add(createCase(nominal1, solverRegistry.solve("integer", op, nominal1), "NORMAL", "HIGH", "Standard nominal integer input", "Verifies primary algorithmic logic under standard integer.", "REQ-1", "REQ-1: Standard Integer", canSolve));

            String nominal2 = "5";
            pool.add(createCase(nominal2, solverRegistry.solve("integer", op, nominal2), "NORMAL", "MEDIUM", "Small positive integer", "Nominal partition verification.", "REQ-1", "REQ-1: Nominal Partition", canSolve));
        }

        // 4. EDGE Cases
        if (allowedCats.contains("EDGE")) {
            String zeroEdge = "0";
            pool.add(createCase(zeroEdge, solverRegistry.solve("integer", op, zeroEdge), "EDGE", "HIGH", "Zero value extreme edge", "Verifies behavior with zero numeric state.", "REQ-3", "REQ-3: Zero State Edge", canSolve));

            String negativeOne = "-1";
            pool.add(createCase(negativeOne, solverRegistry.solve("integer", op, negativeOne), "EDGE", "MEDIUM", "Negative unit edge", "Verifies behavior with negative one transition state.", "REQ-3", "REQ-3: Negative Edge", canSolve));
        }

        // 5. INVALID_INPUT Cases
        if (allowedCats.contains("INVALID_INPUT")) {
            String invalidNeg = "-9999";
            String out = canSolve ? solverRegistry.solve("integer", op, invalidNeg) : "Invalid input error";
            pool.add(createCase(invalidNeg, out, "INVALID_INPUT", "MEDIUM", "Out of bounds negative range", "Negative testing verifying rejection of negative or malformed number.", "REQ-4", "REQ-4: Input Range Validation", canSolve));
        }

        // 6. EXCEPTION Cases
        if (allowedCats.contains("EXCEPTION")) {
            String maxInt = "2147483647";
            String out = canSolve ? solverRegistry.solve("integer", op, maxInt) : "ArithmeticException handled";
            pool.add(createCase(maxInt, out, "EXCEPTION", "LOW", "Integer.MAX_VALUE overflow threshold", "Verifies graceful exception handling during arithmetic overflow.", "REQ-5", "REQ-5: Overflow Threshold", canSolve));
        }

        // Filter by allowed categories
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> tc : pool) {
            if (allowedCats.contains(tc.get("category"))) {
                filtered.add(tc);
            }
        }

        // Fill up to target count if necessary
        int idx = 1;
        while (filtered.size() < testCount) {
            String fillerCat = allowedCats.iterator().next();
            String input = String.valueOf(idx + 10);
            String expected = solverRegistry.solve("integer", op, input);
            filtered.add(createCase(input, expected, fillerCat, "MEDIUM", "Generated numeric variation", "Partition variation test case " + idx, "REQ-1", "REQ-1: Variation " + idx, canSolve));
            idx++;
        }

        // Trim to target count
        if (filtered.size() > testCount) {
            filtered = new ArrayList<>(filtered.subList(0, testCount));
        }

        return filtered;
    }

    private Map<String, Object> createCase(
            String input,
            String expectedOutput,
            String category,
            String priority,
            String preconditions,
            String explanation,
            String reqId,
            String reqTag,
            boolean canSolve
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("input", input);
        m.put("expectedOutput", canSolve ? expectedOutput : "Needs manual review");
        m.put("category", category);
        m.put("priority", priority);
        m.put("preconditions", preconditions);
        m.put("explanation", explanation);
        m.put("requirementId", reqId);
        m.put("requirementTag", reqTag);
        m.put("isReviewed", canSolve ? 1 : 0);
        return m;
    }
}
