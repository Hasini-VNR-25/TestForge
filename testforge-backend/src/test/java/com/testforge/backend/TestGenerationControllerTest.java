package com.testforge.backend;

import com.testforge.backend.controller.TestGenerationController;
import com.testforge.backend.exception.InvalidProblemStatementException;
import com.testforge.backend.service.GeminiService;
import com.testforge.backend.service.ProjectService;
import com.testforge.backend.service.TestQualityService;
import com.testforge.backend.service.generator.OfflineRuleBasedGenerator;
import com.testforge.backend.service.generator.TestGenerationService;
import com.testforge.backend.service.validator.ProblemStatementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestGenerationControllerTest {

    private TestGenerationController controller;
    private ProblemStatementValidator validator;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        validator = new ProblemStatementValidator(null);
        geminiService = new GeminiService();
        OfflineRuleBasedGenerator offlineGen = new OfflineRuleBasedGenerator();
        TestGenerationService genService = new TestGenerationService(null, offlineGen);
        TestQualityService qualityService = new TestQualityService(null);
        // ProjectService can be null or mocked for validation testing since validation runs before DB!
        controller = new TestGenerationController(genService, null, qualityService, validator, geminiService);
    }

    @Test
    @DisplayName("AC 1 & AC 2: Controller throws InvalidProblemStatementException on gibberish before DB insert")
    void testControllerRejectsGibberish() {
        Map<String, Object> req = Map.of(
                "problemStatement", "asdkjhasd qwe zxcv",
                "offline", "true"
        );

        InvalidProblemStatementException ex = assertThrows(
                InvalidProblemStatementException.class,
                () -> controller.generateTests(req)
        );

        assertNotNull(ex.getReason());
        assertFalse(ex.getSuggestions().isEmpty());
    }

    @Test
    @DisplayName("AC 5: ExceptionHandler returns 400 with error, reason, suggestions")
    void testExceptionHandlerReturns400() {
        InvalidProblemStatementException ex = new InvalidProblemStatementException(
                "Problem description contains unnatural consonant sequences.",
                List.of("Check for typos or random keyboard mash.", "Ensure words are spelled correctly in standard English.")
        );

        ResponseEntity<Map<String, Object>> response = controller.handleInvalidProblemStatement(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Problem description contains unnatural consonant sequences.", response.getBody().get("error"));
        assertEquals("Problem description contains unnatural consonant sequences.", response.getBody().get("reason"));
        assertEquals(2, ((List<?>) response.getBody().get("suggestions")).size());
    }

    @Test
    @DisplayName("AC 4: Pre-validation passes for valid description")
    void testControllerValidDescriptionPassesValidation() {
        assertDoesNotThrow(() -> validator.validate("Given a string, return it reversed"));
    }
}
