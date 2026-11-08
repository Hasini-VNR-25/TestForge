package com.testforge.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.backend.exception.InvalidProblemStatementException;
import com.testforge.backend.service.generator.OfflineRuleBasedGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfflineRuleBasedGeneratorTest {

    private OfflineRuleBasedGenerator generator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        generator = new OfflineRuleBasedGenerator();
    }

    @Test
    @DisplayName("AC 7: Offline, 'Reverse a string', constraint 1 ≤ n ≤ 100 generates cases at 1, 100, 0, 101 with real computed outputs")
    void testConstraintDrivenBoundaryValues() throws Exception {
        String problem = "Given a string, return it reversed. Constraint: 1 ≤ n ≤ 100";
        String json = generator.generateTests(problem, 10, List.of("BOUNDARY", "NORMAL"));

        JsonNode root = mapper.readTree(json);
        JsonNode testCases = root.get("testCases");
        assertNotNull(testCases);
        assertTrue(testCases.size() >= 4);

        boolean foundLen1 = false;
        boolean foundLen100 = false;
        boolean foundLen0 = false;
        boolean foundLen101 = false;

        for (JsonNode tc : testCases) {
            String in = tc.path("input").asText();
            String out = tc.path("expectedOutput").asText();
            assertFalse(out.equals("Needs manual review"), "Supported operation should compute real output");

            // Strip quotes if present
            String rawIn = (in.startsWith("\"") && in.endsWith("\"") && in.length() >= 2)
                    ? in.substring(1, in.length() - 1) : in;
            String rawOut = (out.startsWith("\"") && out.endsWith("\"") && out.length() >= 2)
                    ? out.substring(1, out.length() - 1) : out;

            // Verify reversed output matches real reverse
            assertEquals(new StringBuilder(rawIn).reverse().toString(), rawOut);

            if (rawIn.length() == 1) foundLen1 = true;
            if (rawIn.length() == 100) foundLen100 = true;
            if (rawIn.length() == 0) foundLen0 = true;
            if (rawIn.length() == 101) foundLen101 = true;
        }

        assertTrue(foundLen1, "Should have case at min constraint 1");
        assertTrue(foundLen100, "Should have case at max constraint 100");
        assertTrue(foundLen0, "Should have case one below min constraint 0");
        assertTrue(foundLen101, "Should have case one above max constraint 101");
    }

    @Test
    @DisplayName("AC 8: Offline, statement with 'assume' or 'password' is not misclassified as sum or word problems")
    void testNoSubstringFalsePositives() {
        // "assume" should not match "sum", "password" should not match "word"
        // Without real signals, confidence is below threshold and throws InvalidProblemStatementException
        String statement = "Please assume that the user password authentication is verified";
        InvalidProblemStatementException ex = assertThrows(
                InvalidProblemStatementException.class,
                () -> generator.generateTests(statement, 5, null)
        );
        assertNotNull(ex.getReason());
        assertTrue(ex.getSuggestions().stream().anyMatch(s -> s.contains("Supported problem types")));
    }

    @Test
    @DisplayName("AC 9: Offline, recognised type with unsupported operation marks expectedOutput as 'Needs manual review'")
    void testUnsupportedOperationNeedsReview() throws Exception {
        String statement = "Given a string, find the longest uncommon subsequence";
        String json = generator.generateTests(statement, 5, null);

        JsonNode root = mapper.readTree(json);
        JsonNode testCases = root.get("testCases");
        assertNotNull(testCases);

        boolean foundNeedsReview = false;
        for (JsonNode tc : testCases) {
            if ("Needs manual review".equals(tc.path("expectedOutput").asText())) {
                foundNeedsReview = true;
                assertEquals(0, tc.path("isReviewed").asInt(1));
            }
        }
        assertTrue(foundNeedsReview, "Unsupported operation should set expectedOutput to 'Needs manual review'");
    }

    @Test
    @DisplayName("AC 10: Offline, statement below confidence threshold rejected with supported types")
    void testLowConfidenceRejected() {
        String statement = "Hello world welcome to this place";
        InvalidProblemStatementException ex = assertThrows(
                InvalidProblemStatementException.class,
                () -> generator.generateTests(statement, 5, null)
        );
        assertTrue(ex.getReason().contains("confidence"));
        assertTrue(ex.getSuggestions().stream().anyMatch(s -> s.contains("Array operations")));
    }

    @Test
    @DisplayName("AC 11: qualityScore and coverageSummary differ between different inputs and match generated cases")
    void testQualityScoreAndCoverageSummary() throws Exception {
        // Input 1: String reverse with constraints and all categories
        String p1 = "Given a string, return it reversed. Constraint: 1 <= n <= 100";
        String json1 = generator.generateTests(p1, 5, null);
        JsonNode root1 = mapper.readTree(json1);
        int qs1 = root1.path("qualityScore").asInt();
        JsonNode cov1 = root1.get("coverageSummary");

        // Input 2: String with unsupported operation (no constraints)
        String p2 = "Given a string, find the uncommon characters";
        String json2 = generator.generateTests(p2, 5, null);
        JsonNode root2 = mapper.readTree(json2);
        int qs2 = root2.path("qualityScore").asInt();
        JsonNode cov2 = root2.get("coverageSummary");

        // Quality scores must differ based on constraints and solver support
        assertNotEquals(qs1, qs2, "Quality scores should differ across distinct problem specifications");

        // Coverage summary must accurately count categories in testCases
        JsonNode tc1 = root1.get("testCases");
        int countNormal = 0;
        int countBoundary = 0;
        for (JsonNode tc : tc1) {
            String cat = tc.path("category").asText();
            if ("NORMAL".equals(cat)) countNormal++;
            if ("BOUNDARY".equals(cat)) countBoundary++;
        }
        assertEquals(countNormal, cov1.path("NORMAL").asInt(0));
        assertEquals(countBoundary, cov1.path("BOUNDARY").asInt(0));
    }

    @Test
    @DisplayName("Honour requested categories and testCount")
    void testHonourCategoriesAndCount() throws Exception {
        String p = "Given an array of integers, compute the sum of elements";
        String json = generator.generateTests(p, 4, List.of("NORMAL", "BOUNDARY"));

        JsonNode root = mapper.readTree(json);
        JsonNode testCases = root.get("testCases");
        assertEquals(4, testCases.size(), "Must respect requested testCount");

        for (JsonNode tc : testCases) {
            String cat = tc.path("category").asText();
            assertTrue("NORMAL".equals(cat) || "BOUNDARY".equals(cat),
                    "Should only contain requested categories");
        }
    }
}
