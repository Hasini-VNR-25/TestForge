package com.testforge.backend;

import com.testforge.backend.exception.InvalidProblemStatementException;
import com.testforge.backend.service.GeminiService;
import com.testforge.backend.service.generator.GeminiGenerator;
import com.testforge.backend.service.generator.OfflineRuleBasedGenerator;
import com.testforge.backend.service.generator.TestGenerationService;
import com.testforge.backend.service.validator.ProblemStatementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestGenerationFallbackAndSkipTest {

    private ProblemStatementValidator validator;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        validator = new ProblemStatementValidator(null);
        geminiService = new GeminiService();
    }

    @Test
    @DisplayName("AC 5: An invalid statement with Gemini unreachable is still rejected by Layer 1")
    void testInvalidStatementWithGeminiUnreachable() {
        // Even if Gemini is unreachable or unconfigured, Layer 1 catches gibberish
        assertThrows(
                InvalidProblemStatementException.class,
                () -> validator.validate("asdkjhasd qwe zxcv")
        );
    }

    @Test
    @DisplayName("AC 6: A valid statement with Gemini unreachable is accepted with 'Semantic validation skipped' warning")
    void testValidStatementWithGeminiUnreachable() {
        String statement = "Given a string, return it reversed";
        // Layer 1 passes
        assertDoesNotThrow(() -> validator.validate(statement));

        // When Gemini is unreachable or unconfigured, semantic validation returns skipped with warning
        GeminiService.SemanticValidationResult res = geminiService.validateProblemStatement(statement);
        assertTrue(res.skipped(), "Should be marked as skipped when Gemini is unconfigured or unreachable");
        assertTrue(res.warning().contains("Semantic validation skipped"));
        assertTrue(res.valid(), "Must rely on Layer 1 result and not report invalid on network/unreachable");
    }

    @Test
    @DisplayName("AC 12: Gemini failure on a valid statement returns offline result with isFallback flag true")
    void testGeminiFailureFallback() {
        OfflineRuleBasedGenerator offlineGen = new OfflineRuleBasedGenerator();

        // Subclass GeminiGenerator to simulate runtime failure (e.g. 500 error / network disconnect)
        GeminiGenerator failingGeminiGen = new GeminiGenerator(geminiService) {
            @Override
            public boolean isAvailable() {
                return true; // reports available
            }

            @Override
            public String generateTests(String statement, int count, List<String> categories) {
                throw new RuntimeException("503 Service Unavailable / Network Timeout");
            }
        };

        TestGenerationService genService = new TestGenerationService(failingGeminiGen, offlineGen);

        String statement = "Given a string, return it reversed. Constraint: 1 <= n <= 100";
        TestGenerationService.GenerationResult result = genService.generate(statement, 5, null, false);

        assertNotNull(result);
        assertTrue(result.isFallback(), "isFallback must be true when Gemini fails");
        assertEquals("Offline Rule-Based Heuristic Generator", result.engineUsed());
        assertNotNull(result.json());
        assertTrue(result.json().contains("testCases"));
    }
}
