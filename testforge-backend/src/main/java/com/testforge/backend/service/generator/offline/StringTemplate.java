package com.testforge.backend.service.generator.offline;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * B2: Template implementation for string problems.
 */
@Component
public class StringTemplate implements ProblemTemplate {

    @Override
    public String getName() {
        return "StringTemplate";
    }

    @Override
    public double matchScore(AnalysisResult analysis) {
        if ("string".equalsIgnoreCase(analysis.getInputType())) {
            return 1.0;
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
        boolean canSolve = solverRegistry.canSolve("string", op);

        // 1. Examples from problem statement as NORMAL cases
        if (allowedCats.contains("NORMAL")) {
            for (AnalysisResult.ExampleCase ex : analysis.getExamples()) {
                String input = ex.input();
                String output = canSolve ? solverRegistry.solve("string", op, input) : ex.output();
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

                // Length 1 (or min)
                String minStr = generateStringOfLength((int) Math.max(1, min));
                String minOut = solverRegistry.solve("string", op, minStr);
                pool.add(createCase(minStr, minOut, "BOUNDARY", "HIGH", "Lower constraint boundary (length " + min + ")", "Validates boundary condition at exact minimum threshold.", "REQ-2", "REQ-2: Min Boundary", canSolve));

                // Length 100 (or max)
                String maxStr = generateStringOfLength((int) Math.min(max, 500));
                String maxOut = solverRegistry.solve("string", op, maxStr);
                pool.add(createCase(maxStr, maxOut, "BOUNDARY", "HIGH", "Upper constraint boundary (length " + max + ")", "Validates boundary condition at exact maximum threshold.", "REQ-2", "REQ-2: Max Boundary", canSolve));

                // Length 0 (one below min)
                String belowStr = generateStringOfLength((int) Math.max(0, belowMin));
                String belowOut = solverRegistry.solve("string", op, belowStr);
                pool.add(createCase(belowStr, belowOut, "BOUNDARY", "MEDIUM", "One below minimum constraint (length " + belowMin + ")", "Validates boundary condition one unit below allowable minimum.", "REQ-2", "REQ-2: Below Min Boundary", canSolve));

                // Length 101 (one above max)
                String aboveStr = generateStringOfLength((int) Math.min(aboveMax, 501));
                String aboveOut = solverRegistry.solve("string", op, aboveStr);
                pool.add(createCase(aboveStr, aboveOut, "BOUNDARY", "MEDIUM", "One above maximum constraint (length " + aboveMax + ")", "Validates boundary condition one unit above allowable maximum.", "REQ-2", "REQ-2: Above Max Boundary", canSolve));
            } else {
                // Fall back to generic boundary cases for string
                String singleChar = "\"a\"";
                pool.add(createCase(singleChar, solverRegistry.solve("string", op, singleChar), "BOUNDARY", "HIGH", "Single character minimum", "Threshold verification with minimal single character string.", "REQ-2", "REQ-2: Generic Boundary", canSolve));

                String twoChars = "\"ab\"";
                pool.add(createCase(twoChars, solverRegistry.solve("string", op, twoChars), "BOUNDARY", "MEDIUM", "Small two character string", "Threshold boundary verification.", "REQ-2", "REQ-2: Small Boundary", canSolve));
            }
        }

        // 3. NORMAL Cases (if pool still needs more or examples were empty)
        if (allowedCats.contains("NORMAL")) {
            String nominal1 = "\"hello world\"";
            pool.add(createCase(nominal1, solverRegistry.solve("string", op, nominal1), "NORMAL", "HIGH", "Standard nominal string input", "Verifies primary algorithmic logic under standard string.", "REQ-1", "REQ-1: Standard String", canSolve));

            String nominal2 = "\"algorithm\"";
            pool.add(createCase(nominal2, solverRegistry.solve("string", op, nominal2), "NORMAL", "MEDIUM", "Arbitrary alphabetic string", "Nominal partition verification.", "REQ-1", "REQ-1: Nominal Partition", canSolve));
        }

        // 4. EDGE Cases
        if (allowedCats.contains("EDGE")) {
            String emptyStr = "\"\"";
            pool.add(createCase(emptyStr, solverRegistry.solve("string", op, emptyStr), "EDGE", "HIGH", "Empty string structural edge", "Verifies handling of empty or 0-length string state.", "REQ-3", "REQ-3: Empty String", canSolve));

            String whitespaceStr = "\"   \"";
            pool.add(createCase(whitespaceStr, solverRegistry.solve("string", op, whitespaceStr), "EDGE", "LOW", "Whitespace string edge", "Verifies behavior with whitespace-only characters.", "REQ-3", "REQ-3: Whitespace Edge", canSolve));
        }

        // 5. INVALID_INPUT Cases
        if (allowedCats.contains("INVALID_INPUT")) {
            String invalidSymbols = "\"!@#$%^&*()\"";
            String out = canSolve ? solverRegistry.solve("string", op, invalidSymbols) : "Invalid input error";
            pool.add(createCase(invalidSymbols, out, "INVALID_INPUT", "MEDIUM", "Special character / symbol sequence", "Negative testing verifying resilience to unexpected characters.", "REQ-4", "REQ-4: Symbol Validation", canSolve));
        }

        // 6. EXCEPTION Cases
        if (allowedCats.contains("EXCEPTION")) {
            String largeStr = "\"" + "a".repeat(1000) + "\"";
            String out = canSolve ? solverRegistry.solve("string", op, largeStr) : "MemoryLimitExceeded / Handled";
            pool.add(createCase(largeStr, out, "EXCEPTION", "LOW", "Stress / scale memory threshold", "Stress test verifying execution limits under long input sequence.", "REQ-5", "REQ-5: Memory Stress", canSolve));
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
            String input = "\"testcase_" + idx + "\"";
            String expected = solverRegistry.solve("string", op, input);
            filtered.add(createCase(input, expected, fillerCat, "MEDIUM", "Generated variation", "Partition variation test case " + idx, "REQ-1", "REQ-1: Variation " + idx, canSolve));
            idx++;
        }

        // Trim to target count
        if (filtered.size() > testCount) {
            filtered = new ArrayList<>(filtered.subList(0, testCount));
        }

        return filtered;
    }

    private String generateStringOfLength(int len) {
        if (len <= 0) return "\"\"";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return "\"" + sb.toString() + "\"";
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
