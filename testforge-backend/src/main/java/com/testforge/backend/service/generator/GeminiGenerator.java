package com.testforge.backend.service.generator;

import com.testforge.backend.exception.GenerationFailedException;
import com.testforge.backend.service.GeminiService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cloud LLM test generator powered by Google's Gemini API.
 */
@Component
public class GeminiGenerator implements TestCaseGenerator {

    private final GeminiService geminiService;

    public GeminiGenerator(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Override
    public String generateTests(String problemStatement, int testCount, List<String> categories) throws GenerationFailedException {
        try {
            return geminiService.generateTests(problemStatement, testCount, categories);
        } catch (Exception e) {
            throw new GenerationFailedException("Gemini AI generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getGeneratorType() {
        return "Gemini AI Generator";
    }

    @Override
    public boolean isAvailable() {
        return geminiService.isConfigured();
    }
}
