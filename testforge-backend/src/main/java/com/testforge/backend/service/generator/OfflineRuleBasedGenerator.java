package com.testforge.backend.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.backend.exception.GenerationFailedException;
import com.testforge.backend.exception.InvalidProblemStatementException;
import com.testforge.backend.service.generator.offline.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Reworked Offline Rule-Based Heuristic Generator.
 * Implements TestCaseGenerator using ProblemAnalyzer, ProblemTemplate strategy,
 * ReferenceSolverRegistry, and constraint-driven boundary value analysis.
 */
@Component
public class OfflineRuleBasedGenerator implements TestCaseGenerator {

    private static final double CONFIDENCE_THRESHOLD = 0.35;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProblemAnalyzer analyzer;
    private final ProblemTemplateRegistry templateRegistry;
    private final ReferenceSolverRegistry solverRegistry;

    public OfflineRuleBasedGenerator(
            ProblemAnalyzer analyzer,
            ProblemTemplateRegistry templateRegistry,
            ReferenceSolverRegistry solverRegistry
    ) {
        this.analyzer = analyzer != null ? analyzer : new ProblemAnalyzer();
        this.templateRegistry = templateRegistry != null ? templateRegistry : new ProblemTemplateRegistry(null);
        this.solverRegistry = solverRegistry != null ? solverRegistry : new ReferenceSolverRegistry();
    }

    public OfflineRuleBasedGenerator() {
        this(new ProblemAnalyzer(), new ProblemTemplateRegistry(null), new ReferenceSolverRegistry());
    }

    @Override
    public String generateTests(String problemStatement, int testCount, List<String> categories) throws GenerationFailedException {
        try {
            // Respect testCount (1-25)
            int count = (testCount >= 1 && testCount <= 25) ? testCount : 5;

            // B1: Analyze statement using word-boundary regex
            AnalysisResult analysis = analyzer.analyze(problemStatement);

            // B6: Low-confidence refusal
            if (analysis.getConfidenceScore() < CONFIDENCE_THRESHOLD) {
                throw new InvalidProblemStatementException(
                        "The offline rule-based generator could not classify the problem statement with sufficient confidence (confidence: "
                                + String.format("%.2f", analysis.getConfidenceScore()) + ").",
                        List.of(
                                "Supported problem types: Array operations (reverse, sum, sort, palindrome), String operations (reverse, palindrome, sort), Numeric operations (sum, factorial, prime, fibonacci).",
                                "Specify the input type clearly (e.g., 'Given an array...', 'Given a string...').",
                                "Specify the operation clearly (e.g., 'reverse', 'sum', 'sort', 'is palindrome')."
                        )
                );
            }

            // B2: Select template with highest classifier score
            ProblemTemplate template = templateRegistry.getBestTemplate(analysis);

            // B3, B4, B5: Generate test cases honoring categories, constraints, and solver
            List<Map<String, Object>> testCases = template.generateTestCases(analysis, count, categories, solverRegistry);

            // B5: Compute coverageSummary dynamically from actual test cases
            Map<String, Integer> coverageSummary = new LinkedHashMap<>();
            for (Map<String, Object> tc : testCases) {
                String cat = (String) tc.getOrDefault("category", "NORMAL");
                coverageSummary.put(cat, coverageSummary.getOrDefault(cat, 0) + 1);
            }

            // B5: Compute qualityScore dynamically based on actual coverage
            int qualityScore = calculateQualityScore(testCases, analysis);

            // Dynamic requirements traceability list
            List<Map<String, String>> requirements = new ArrayList<>();
            requirements.add(Map.of("id", "REQ-1", "title", "Core Nominal Execution", "description", "Process valid " + analysis.getInputType() + " inputs correctly"));
            if (coverageSummary.containsKey("BOUNDARY")) {
                requirements.add(Map.of("id", "REQ-2", "title", "Boundary Limits", "description", "Enforce threshold boundaries for " + (analysis.hasConstraints() ? analysis.getConstraintVar() : "input size")));
            }
            if (coverageSummary.containsKey("EDGE")) {
                requirements.add(Map.of("id", "REQ-3", "title", "Structural Extremes", "description", "Handle empty or minimal " + analysis.getInputType() + " structures"));
            }
            if (coverageSummary.containsKey("INVALID_INPUT")) {
                requirements.add(Map.of("id", "REQ-4", "title", "Input Validation", "description", "Reject malformed or out-of-bounds inputs"));
            }
            if (coverageSummary.containsKey("EXCEPTION")) {
                requirements.add(Map.of("id", "REQ-5", "title", "Stress & Exception Limits", "description", "Maintain resilience under large scale stress or overflow"));
            }

            // B7: Honest labelling warning
            List<String> warnings = new ArrayList<>();
            warnings.add("Generated via Offline Rule-Based Heuristic Engine. Covers recognised problem types only.");
            if (testCases.stream().anyMatch(tc -> "Needs manual review".equals(tc.get("expectedOutput")))) {
                warnings.add("Some expected outputs could not be determined algorithmically and require manual review.");
            }

            // Construct full JSON object
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("qualityScore", qualityScore);
            root.put("coverageSummary", coverageSummary);
            root.put("requirements", requirements);
            root.put("potentialGaps", List.of("Concurrency safety not formally specified in problem statement."));
            root.put("warnings", warnings);
            root.put("testCases", testCases);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        } catch (InvalidProblemStatementException e) {
            // Propagate validation failure directly
            throw e;
        } catch (Exception e) {
            throw new GenerationFailedException("Offline rule generation failed: " + e.getMessage(), e);
        }
    }

    private int calculateQualityScore(List<Map<String, Object>> testCases, AnalysisResult analysis) {
        if (testCases == null || testCases.isEmpty()) return 50;

        int score = 55; // Base score

        // Category diversity (+5 per distinct category)
        Set<String> categoriesPresent = new HashSet<>();
        for (Map<String, Object> tc : testCases) {
            categoriesPresent.add((String) tc.get("category"));
        }
        score += categoriesPresent.size() * 5;

        // Solved outputs bonus (+10 if all expected outputs are solved)
        boolean hasUnsolved = testCases.stream()
                .anyMatch(tc -> "Needs manual review".equals(tc.get("expectedOutput")));
        if (!hasUnsolved) {
            score += 10;
        }

        // Constraints bonus (+10 if explicit constraints were extracted and tested)
        if (analysis.hasConstraints()) {
            score += 10;
        }

        // Test count factor (+1 per case up to 10)
        score += Math.min(testCases.size(), 10);

        return Math.min(98, Math.max(50, score));
    }

    @Override
    public String getGeneratorType() {
        return "Offline Rule-Based Heuristic Generator";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public ProblemAnalyzer getAnalyzer() {
        return analyzer;
    }
}
