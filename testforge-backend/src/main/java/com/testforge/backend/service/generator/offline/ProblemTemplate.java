package com.testforge.backend.service.generator.offline;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for problem domain test synthesis.
 * New templates can be added without modifying existing templates.
 */
public interface ProblemTemplate {

    String getName();

    double matchScore(AnalysisResult analysis);

    List<Map<String, Object>> generateTestCases(
            AnalysisResult analysis,
            int testCount,
            List<String> categories,
            ReferenceSolverRegistry solverRegistry
    );
}
