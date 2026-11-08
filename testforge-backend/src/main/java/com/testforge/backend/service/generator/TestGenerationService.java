package com.testforge.backend.service.generator;

import com.testforge.backend.exception.GenerationFailedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service orchestrating polymorphic test case generation.
 * Selects between GeminiGenerator and OfflineRuleBasedGenerator with automatic fallback.
 */
@Service
public class TestGenerationService {

    private final GeminiGenerator geminiGenerator;
    private final OfflineRuleBasedGenerator offlineGenerator;

    public TestGenerationService(GeminiGenerator geminiGenerator, OfflineRuleBasedGenerator offlineGenerator) {
        this.geminiGenerator = geminiGenerator;
        this.offlineGenerator = offlineGenerator;
    }

    public GenerationResult generate(String problemStatement, int testCount, List<String> categories, boolean forceOffline) {
        if (forceOffline || !geminiGenerator.isAvailable()) {
            String json = offlineGenerator.generateTests(problemStatement, testCount, categories);
            return new GenerationResult(json, offlineGenerator.getGeneratorType(), false);
        }

        try {
            String json = geminiGenerator.generateTests(problemStatement, testCount, categories);
            return new GenerationResult(json, geminiGenerator.getGeneratorType(), false);
        } catch (Exception e) {
            // Graceful fallback to offline rule-based generator
            String json = offlineGenerator.generateTests(problemStatement, testCount, categories);
            return new GenerationResult(json, offlineGenerator.getGeneratorType(), true);
        }
    }

    public static record GenerationResult(String json, String engineUsed, boolean isFallback) {}
}
