package com.testforge.backend.service.generator.offline;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * B2: Template implementation for array and matrix problems.
 */
@Component
public class ArrayTemplate implements ProblemTemplate {

    @Override
    public String getName() {
        return "ArrayTemplate";
    }

    @Override
    public double matchScore(AnalysisResult analysis) {
        String type = analysis.getInputType();
        if ("array".equalsIgnoreCase(type)) {
            return 1.0;
        }
        if ("matrix".equalsIgnoreCase(type)) {
            return 0.9;
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
        boolean canSolve = solverRegistry.canSolve("array", op);

        // 1. Examples from problem statement as NORMAL cases
        if (allowedCats.contains("NORMAL")) {
            for (AnalysisResult.ExampleCase ex : analysis.getExamples()) {
                String input = ex.input();
                String output = canSolve ? solverRegistry.solve("array", op, input) : ex.output();
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

                // Min length or value
                String minArr = generateArrayOfLength((int) Math.max(1, min));
                String minOut = solverRegistry.solve("array", op, minArr);
                pool.add(createCase(minArr, minOut, "BOUNDARY", "HIGH", "Minimum threshold boundary (size " + min + ")", "Validates boundary condition at exact minimum threshold.", "REQ-2", "REQ-2: Min Boundary", canSolve));

                // Max length or value
                String maxArr = generateArrayOfLength((int) Math.min(max, 50));
                String maxOut = solverRegistry.solve("array", op, maxArr);
                pool.add(createCase(maxArr, maxOut, "BOUNDARY", "HIGH", "Maximum threshold boundary (size " + max + ")", "Validates boundary condition at exact maximum threshold.", "REQ-2", "REQ-2: Max Boundary", canSolve));

                // Below min (e.g. empty or 0 length)
                String belowArr = generateArrayOfLength((int) Math.max(0, belowMin));
                String belowOut = solverRegistry.solve("array", op, belowArr);
                pool.add(createCase(belowArr, belowOut, "BOUNDARY", "MEDIUM", "One below minimum threshold (size " + belowMin + ")", "Validates boundary condition one unit below allowable minimum.", "REQ-2", "REQ-2: Below Min Boundary", canSolve));

                // Above max
                String aboveArr = generateArrayOfLength((int) Math.min(aboveMax, 51));
                String aboveOut = solverRegistry.solve("array", op, aboveArr);
                pool.add(createCase(aboveArr, aboveOut, "BOUNDARY", "MEDIUM", "One above maximum threshold (size " + aboveMax + ")", "Validates boundary condition one unit above allowable maximum.", "REQ-2", "REQ-2: Above Max Boundary", canSolve));
            } else {
                // Fall back to generic boundary cases for array
                String singleElem = "[1]";
                pool.add(createCase(singleElem, solverRegistry.solve("array", op, singleElem), "BOUNDARY", "HIGH", "Single element boundary", "Lower bound threshold for single element array.", "REQ-2", "REQ-2: Single Element Boundary", canSolve));

                String twoElems = "[0, 0]";
                pool.add(createCase(twoElems, solverRegistry.solve("array", op, twoElems), "BOUNDARY", "MEDIUM", "Identical elements threshold", "Boundary verification with duplicate zero elements.", "REQ-2", "REQ-2: Zero Element Boundary", canSolve));
            }
        }

        // 3. NORMAL Cases
        if (allowedCats.contains("NORMAL")) {
            String nominal1 = "[2, 7, 11, 15]";
            pool.add(createCase(nominal1, solverRegistry.solve("array", op, nominal1), "NORMAL", "HIGH", "Standard nominal array input", "Verifies primary algorithmic logic under standard array.", "REQ-1", "REQ-1: Standard Array", canSolve));

            String nominal2 = "[1, 3, 5, 7, 9]";
            pool.add(createCase(nominal2, solverRegistry.solve("array", op, nominal2), "NORMAL", "MEDIUM", "Odd sequence array", "Nominal partition verification.", "REQ-1", "REQ-1: Nominal Partition", canSolve));
        }

        // 4. EDGE Cases
        if (allowedCats.contains("EDGE")) {
            String emptyArr = "[]";
            pool.add(createCase(emptyArr, solverRegistry.solve("array", op, emptyArr), "EDGE", "HIGH", "Empty array structural extreme", "Verifies behavior when input array contains zero elements.", "REQ-3", "REQ-3: Empty Array", canSolve));

            String singleZero = "[0]";
            pool.add(createCase(singleZero, solverRegistry.solve("array", op, singleZero), "EDGE", "MEDIUM", "Single zero element", "Verifies behavior with minimal zero value.", "REQ-3", "REQ-3: Zero Edge", canSolve));
        }

        // 5. INVALID_INPUT Cases
        if (allowedCats.contains("INVALID_INPUT")) {
            String invalidArr = "[null, -999]";
            String out = canSolve ? solverRegistry.solve("array", op, invalidArr) : "Invalid input error";
            pool.add(createCase(invalidArr, out, "INVALID_INPUT", "MEDIUM", "Null / out of bounds elements", "Negative testing verifying rejection of malformed or invalid array values.", "REQ-4", "REQ-4: Input Validation", canSolve));
        }

        // 6. EXCEPTION Cases
        if (allowedCats.contains("EXCEPTION")) {
            String overflowArr = "[2147483647, 2147483647]";
            String out = canSolve ? solverRegistry.solve("array", op, overflowArr) : "ArithmeticException handled";
            pool.add(createCase(overflowArr, out, "EXCEPTION", "LOW", "Integer overflow stress limits", "Verifies graceful exception handling during arithmetic overflow or memory stress.", "REQ-5", "REQ-5: Overflow Exception", canSolve));
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
            String input = "[" + idx + ", " + (idx * 2) + ", " + (idx * 3) + "]";
            String expected = solverRegistry.solve("array", op, input);
            filtered.add(createCase(input, expected, fillerCat, "MEDIUM", "Generated array variation", "Partition variation test case " + idx, "REQ-1", "REQ-1: Variation " + idx, canSolve));
            idx++;
        }

        // Trim to target count
        if (filtered.size() > testCount) {
            filtered = new ArrayList<>(filtered.subList(0, testCount));
        }

        return filtered;
    }

    private String generateArrayOfLength(int len) {
        if (len <= 0) return "[]";
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= len; i++) {
            list.add(String.valueOf(i));
        }
        return "[" + String.join(", ", list) + "]";
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
