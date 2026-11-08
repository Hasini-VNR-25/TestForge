package com.testforge.backend.service.generator;

import com.testforge.backend.exception.GenerationFailedException;
import java.util.List;

/**
 * Abstraction for test generation engines.
 * Enables polymorphism between cloud AI models and local heuristic fallback engines.
 */
public interface TestCaseGenerator {

    /**
     * Generates candidate test cases and quality analysis for a given problem statement.
     *
     * @param problemStatement The functional problem specification
     * @param testCount        Target count of test cases (e.g. 5 to 25)
     * @param categories       Requested categories (e.g. NORMAL, BOUNDARY, etc.)
     * @return Raw JSON containing testCases and quality metrics
     * @throws GenerationFailedException if generation fails
     */
    String generateTests(String problemStatement, int testCount, List<String> categories) throws GenerationFailedException;

    /**
     * Identifier name of this generator strategy.
     */
    String getGeneratorType();

    /**
     * Checks if this generator is currently available and ready to generate.
     */
    boolean isAvailable();
}
