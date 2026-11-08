package com.testforge.backend.controller;

import com.testforge.backend.config.ConfigLoader;
import com.testforge.backend.exception.InvalidProblemStatementException;
import com.testforge.backend.model.Problem;
import com.testforge.backend.service.GeminiService;
import com.testforge.backend.service.ProjectService;
import com.testforge.backend.service.TestQualityService;
import com.testforge.backend.service.generator.TestGenerationService;
import com.testforge.backend.service.validator.ProblemStatementValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/problems")
public class TestGenerationController {

    private final TestGenerationService generationService;
    private final ProjectService projectService;
    private final TestQualityService qualityService;
    private final ProblemStatementValidator validator;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
            .enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            .enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature())
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public TestGenerationController(
            TestGenerationService generationService,
            ProjectService projectService,
            TestQualityService qualityService,
            ProblemStatementValidator validator,
            GeminiService geminiService
    ) {
        this.generationService = generationService;
        this.projectService = projectService;
        this.qualityService = qualityService;
        this.validator = validator;
        this.geminiService = geminiService;
    }

    private String extractRawDescription(String statement, String explicitDesc) {
        if (explicitDesc != null && !explicitDesc.isBlank()) {
            return explicitDesc.trim();
        }
        if (statement == null) return "";
        String marker = "Problem description:\n";
        int idx = statement.indexOf(marker);
        if (idx != -1) {
            String after = statement.substring(idx + marker.length());
            int nextSection = after.indexOf("\n\n");
            if (nextSection != -1) {
                return after.substring(0, nextSection).trim();
            }
            return after.trim();
        }
        return statement.trim();
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }

        // Sanitize any Python-style string multiplication expressions like "a" * 1000
        trimmed = trimmed.replaceAll("\"([^\"]*)\"\\s*\\*\\s*(\\d+)", "\"$1 repeated $2 times\"");

        return trimmed;
    }

    @PostMapping("/generate-tests")
    public Map<String, Object> generateTests(
            @RequestBody Map<String, Object> request) throws Exception {

        String statement = (String) request.get("problemStatement");
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Problem statement is required.");
        }

        String title = (String) request.getOrDefault("title", "Generated Problem");
        if (title == null || title.isBlank()) {
            title = "Generated Problem";
        }

        // Load default test count from ConfigLoader (configured in testforge.properties)
        int defaultTestCount = ConfigLoader.getInstance().getIntProperty("testforge.defaults.test-count", 5);
        int testCount = defaultTestCount;
        Object countObj = request.get("testCount");
        if (countObj != null) {
            try {
                testCount = Integer.parseInt(countObj.toString());
            } catch (NumberFormatException ignored) {}
        }
        if (testCount < 1) testCount = defaultTestCount;
        if (testCount > 25) testCount = 25;

        List<String> categories = null;
        Object catsObj = request.get("categories");
        if (catsObj instanceof List<?>) {
            categories = ((List<?>) catsObj).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
        }
        if (categories == null || categories.isEmpty()) {
            categories = ConfigLoader.getInstance().getListProperty("testforge.defaults.categories");
        }

        boolean forceOffline = Boolean.parseBoolean(String.valueOf(request.getOrDefault("offline", "false")));

        // A4: Validate raw description before any generation or fallback try/catch
        String rawDescription = extractRawDescription(statement, (String) request.get("description"));

        // Layer 1 Validation: Local deterministic rule chain (runs for both online and offline)
        validator.validate(rawDescription);

        // Layer 2 Validation: Gemini semantic validation (online mode only)
        String semanticWarning = null;
        if (!forceOffline && geminiService.isConfigured()) {
            GeminiService.SemanticValidationResult semResult = geminiService.validateProblemStatement(rawDescription);
            if (!semResult.valid() && !semResult.skipped()) {
                throw new InvalidProblemStatementException(
                        semResult.reason() != null && !semResult.reason().isBlank()
                                ? semResult.reason()
                                : "Problem statement failed semantic validation.",
                        semResult.suggestions()
                );
            }
            if (semResult.skipped()) {
                semanticWarning = "Semantic validation skipped";
            }
        }

        // Generate test cases and quality analysis via polymorphic generator (Gemini with Offline Fallback)
        TestGenerationService.GenerationResult genResult = generationService.generate(statement, testCount, categories, forceOffline);
        String result = cleanJson(genResult.json());

        // Parse generator's JSON response
        JsonNode root = objectMapper.readTree(result);
        JsonNode testCases = root.get("testCases");

        if (testCases == null || !testCases.isArray()) {
            throw new IllegalArgumentException(
                "Generation response does not contain a testCases array."
            );
        }

        int qualityScore = root.path("qualityScore").asInt(85);

        // Run algorithmic quality & duplicate audit
        List<Map<String, Object>> auditCases = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            JsonNode tc = testCases.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("input", tc.path("input").asText(""));
            m.put("expectedOutput", tc.path("expectedOutput").asText(""));
            m.put("explanation", tc.path("explanation").asText(""));
            m.put("category", tc.path("category").asText("NORMAL"));
            auditCases.add(m);
        }
        TestQualityService.QualityAuditResult qualityAudit = qualityService.auditTestCases(auditCases);

        // Merge warnings including semantic validation skip warning or fallback warning
        List<String> warningsList = new ArrayList<>();
        if (root.has("warnings") && root.get("warnings").isArray()) {
            for (JsonNode w : root.get("warnings")) {
                warningsList.add(w.asText());
            }
        }
        if (semanticWarning != null && !warningsList.contains(semanticWarning)) {
            warningsList.add(semanticWarning);
        }
        if (genResult.isFallback()) {
            String fallbackWarning = "Fallback: Gemini AI was unreachable or failed. Generated using Offline Rule-Based Engine.";
            if (!warningsList.contains(fallbackWarning)) {
                warningsList.add(fallbackWarning);
            }
        }

        Map<String, Object> fullSummaryObj = new HashMap<>();
        fullSummaryObj.put("qualityScore", qualityScore);
        fullSummaryObj.put("coverageSummary", root.get("coverageSummary"));
        fullSummaryObj.put("requirements", root.get("requirements"));
        fullSummaryObj.put("requirementsAnalyzed", root.get("requirementsAnalyzed"));
        fullSummaryObj.put("potentialGaps", root.get("potentialGaps"));
        fullSummaryObj.put("warnings", warningsList);
        fullSummaryObj.put("qualityAudit", qualityAudit);
        String coverageSummaryJson = objectMapper.writeValueAsString(fullSummaryObj);

        String settingsStr = "Count: " + testCases.size() + "; Categories: " + (categories != null ? String.join(",", categories) : "ALL");

        long problemId;

        try (Connection con = projectService.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Get the next problem ID
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT TF_PROBLEMS_SEQ.NEXTVAL FROM DUAL")) {
                    rs.next();
                    problemId = rs.getLong(1);
                }

                // Save the problem with metadata, quality score and coverage summary
                String insertProblemSql = "INSERT INTO TF_PROBLEMS " +
                        "(PROBLEM_ID, TITLE, DESCRIPTION, CREATED_AT, TEST_COUNT, SETTINGS, QUALITY_SCORE, COVERAGE_SUMMARY) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(insertProblemSql)) {
                    ps.setLong(1, problemId);
                    ps.setString(2, title);
                    ps.setString(3, statement);
                    ps.setInt(4, testCases.size());
                    ps.setString(5, settingsStr);
                    ps.setInt(6, qualityScore);
                    ps.setString(7, coverageSummaryJson);
                    ps.executeUpdate();
                }

                // Save every generated test case with full structured attributes
                String insertTestCaseSql = "INSERT INTO TF_TEST_CASES " +
                        "(TEST_CASE_ID, PROBLEM_ID, INPUT_DATA, EXPECTED_OUTPUT, CATEGORY, EXPLANATION, " +
                        "PRIORITY, PRECONDITIONS, IS_REVIEWED, REQUIREMENT_TAG) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";

                for (JsonNode tc : testCases) {
                    long testCaseId;
                    try (Statement st = con.createStatement();
                         ResultSet rs = st.executeQuery("SELECT TF_TEST_CASES_SEQ.NEXTVAL FROM DUAL")) {
                        rs.next();
                        testCaseId = rs.getLong(1);
                    }

                    try (PreparedStatement ps = con.prepareStatement(insertTestCaseSql)) {
                        ps.setLong(1, testCaseId);
                        ps.setLong(2, problemId);
                        ps.setString(3, tc.path("input").asText(""));
                        ps.setString(4, tc.path("expectedOutput").asText(""));
                        ps.setString(5, tc.path("category").asText("NORMAL").toUpperCase());
                        ps.setString(6, tc.path("explanation").asText(""));
                        ps.setString(7, tc.path("priority").asText("MEDIUM").toUpperCase());
                        ps.setString(8, tc.path("preconditions").asText("Standard Execution"));
                        ps.setString(9, tc.path("requirementTag").asText(""));
                        ps.executeUpdate();
                    }
                }

                con.commit();

                // Persist into InMemoryRepository cache and JsonFileRepository backup
                Problem problemModel = new Problem();
                problemModel.setId(problemId);
                problemModel.setTitle(title);
                problemModel.setDescription(statement);
                problemModel.setTestCount(testCases.size());
                problemModel.setSettings(settingsStr);
                problemModel.setQualityScore(qualityScore);
                problemModel.setCoverageSummary(coverageSummaryJson);
                problemModel.setCreatedAt(new Date());
                projectService.saveProblemToRepositories(problemModel);

            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", problemId);
        response.put("title", title);
        response.put("qualityScore", qualityScore);
        response.put("testCount", testCases.size());
        response.put("engineUsed", genResult.engineUsed());
        response.put("isFallback", genResult.isFallback());
        response.put("result", result); // Preserves compatibility with existing frontend
        response.put("coverageSummary", root.get("coverageSummary"));
        response.put("requirements", root.get("requirements"));
        response.put("requirementsAnalyzed", root.get("requirementsAnalyzed"));
        response.put("potentialGaps", root.get("potentialGaps"));
        response.put("warnings", warningsList);
        response.put("qualityAudit", qualityAudit);

        return response;
    }

    @PostMapping("/{id}/regenerate")
    public Map<String, Object> regenerateTests(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request) throws Exception {

        if (request == null) request = Collections.emptyMap();

        // 1. Retrieve the existing problem
        Map<String, Object> projectDetails = projectService.getProjectDetails(id);
        Problem existingProblem = (Problem) projectDetails.get("project");
        if (existingProblem == null) {
            throw new NoSuchElementException("Problem not found with ID: " + id);
        }

        String statement = (String) request.get("problemStatement");
        if (statement == null || statement.isBlank()) {
            statement = existingProblem.getDescription();
        }
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Problem statement is required.");
        }

        String title = (String) request.get("title");
        if (title == null || title.isBlank()) {
            title = existingProblem.getTitle();
        }

        int defaultTestCount = existingProblem.getTestCount() != null && existingProblem.getTestCount() > 0
                ? existingProblem.getTestCount()
                : ConfigLoader.getInstance().getIntProperty("testforge.defaults.test-count", 5);

        int testCount = defaultTestCount;
        Object countObj = request.get("testCount");
        if (countObj != null) {
            try {
                testCount = Integer.parseInt(countObj.toString());
            } catch (NumberFormatException ignored) {}
        }
        if (testCount < 1) testCount = defaultTestCount;
        if (testCount > 25) testCount = 25;

        List<String> categories = null;
        Object catsObj = request.get("categories");
        if (catsObj instanceof List<?>) {
            categories = ((List<?>) catsObj).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
        }
        if (categories == null || categories.isEmpty()) {
            String settings = existingProblem.getSettings();
            if (settings != null && settings.contains("Categories: ")) {
                String catsPart = settings.substring(settings.indexOf("Categories: ") + 12).trim();
                if (!catsPart.isEmpty() && !"ALL".equalsIgnoreCase(catsPart)) {
                    categories = Arrays.asList(catsPart.split(","));
                }
            }
        }
        if (categories == null || categories.isEmpty()) {
            categories = ConfigLoader.getInstance().getListProperty("testforge.defaults.categories");
        }

        boolean forceOffline = Boolean.parseBoolean(String.valueOf(request.getOrDefault("offline", "false")));

        // Validate raw description before generation
        String rawDescription = extractRawDescription(statement, (String) request.get("description"));
        validator.validate(rawDescription);

        String semanticWarning = null;
        if (!forceOffline && geminiService.isConfigured()) {
            GeminiService.SemanticValidationResult semResult = geminiService.validateProblemStatement(rawDescription);
            if (!semResult.valid() && !semResult.skipped()) {
                throw new InvalidProblemStatementException(
                        semResult.reason() != null && !semResult.reason().isBlank()
                                ? semResult.reason()
                                : "Problem statement failed semantic validation.",
                        semResult.suggestions()
                );
            }
            if (semResult.skipped()) {
                semanticWarning = "Semantic validation skipped";
            }
        }

        // Generate test cases (with offline fallback if wifi is disconnected / Gemini fails)
        TestGenerationService.GenerationResult genResult = generationService.generate(statement, testCount, categories, forceOffline);
        String result = cleanJson(genResult.json());

        JsonNode root = objectMapper.readTree(result);
        JsonNode testCases = root.get("testCases");
        if (testCases == null || !testCases.isArray()) {
            throw new IllegalArgumentException("Generation response does not contain a testCases array.");
        }

        int qualityScore = root.path("qualityScore").asInt(85);

        List<Map<String, Object>> auditCases = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            JsonNode tc = testCases.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("input", tc.path("input").asText(""));
            m.put("expectedOutput", tc.path("expectedOutput").asText(""));
            m.put("explanation", tc.path("explanation").asText(""));
            m.put("category", tc.path("category").asText("NORMAL"));
            auditCases.add(m);
        }
        TestQualityService.QualityAuditResult qualityAudit = qualityService.auditTestCases(auditCases);

        List<String> warningsList = new ArrayList<>();
        if (root.has("warnings") && root.get("warnings").isArray()) {
            for (JsonNode w : root.get("warnings")) {
                warningsList.add(w.asText());
            }
        }
        if (semanticWarning != null && !warningsList.contains(semanticWarning)) {
            warningsList.add(semanticWarning);
        }
        if (genResult.isFallback()) {
            String fallbackWarning = "Fallback: Gemini AI was unreachable or failed. Generated using Offline Rule-Based Engine.";
            if (!warningsList.contains(fallbackWarning)) {
                warningsList.add(fallbackWarning);
            }
        }

        Map<String, Object> fullSummaryObj = new HashMap<>();
        fullSummaryObj.put("qualityScore", qualityScore);
        fullSummaryObj.put("coverageSummary", root.get("coverageSummary"));
        fullSummaryObj.put("requirements", root.get("requirements"));
        fullSummaryObj.put("requirementsAnalyzed", root.get("requirementsAnalyzed"));
        fullSummaryObj.put("potentialGaps", root.get("potentialGaps"));
        fullSummaryObj.put("warnings", warningsList);
        fullSummaryObj.put("qualityAudit", qualityAudit);
        String coverageSummaryJson = objectMapper.writeValueAsString(fullSummaryObj);

        String settingsStr = "Count: " + testCases.size() + "; Categories: " + (categories != null ? String.join(",", categories) : "ALL");

        // Database transaction: Delete old test cases and insert new ones
        try (Connection con = projectService.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Delete previous test cases for this problem
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM TF_TEST_CASES WHERE PROBLEM_ID = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }

                // Update problem record
                String updateProblemSql = "UPDATE TF_PROBLEMS SET " +
                        "TITLE = ?, DESCRIPTION = ?, TEST_COUNT = ?, SETTINGS = ?, QUALITY_SCORE = ?, COVERAGE_SUMMARY = ? " +
                        "WHERE PROBLEM_ID = ?";
                try (PreparedStatement ps = con.prepareStatement(updateProblemSql)) {
                    ps.setString(1, title);
                    ps.setString(2, statement);
                    ps.setInt(3, testCases.size());
                    ps.setString(4, settingsStr);
                    ps.setInt(5, qualityScore);
                    ps.setString(6, coverageSummaryJson);
                    ps.setLong(7, id);
                    ps.executeUpdate();
                }

                // Insert new test cases
                String insertTestCaseSql = "INSERT INTO TF_TEST_CASES " +
                        "(TEST_CASE_ID, PROBLEM_ID, INPUT_DATA, EXPECTED_OUTPUT, CATEGORY, EXPLANATION, " +
                        "PRIORITY, PRECONDITIONS, IS_REVIEWED, REQUIREMENT_TAG) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";

                for (JsonNode tc : testCases) {
                    long testCaseId;
                    try (Statement st = con.createStatement();
                         ResultSet rs = st.executeQuery("SELECT TF_TEST_CASES_SEQ.NEXTVAL FROM DUAL")) {
                        rs.next();
                        testCaseId = rs.getLong(1);
                    }

                    try (PreparedStatement ps = con.prepareStatement(insertTestCaseSql)) {
                        ps.setLong(1, testCaseId);
                        ps.setLong(2, id);
                        ps.setString(3, tc.path("input").asText(""));
                        ps.setString(4, tc.path("expectedOutput").asText(""));
                        ps.setString(5, tc.path("category").asText("NORMAL").toUpperCase());
                        ps.setString(6, tc.path("explanation").asText(""));
                        ps.setString(7, tc.path("priority").asText("MEDIUM").toUpperCase());
                        ps.setString(8, tc.path("preconditions").asText("Standard Execution"));
                        ps.setString(9, tc.path("requirementTag").asText(""));
                        ps.executeUpdate();
                    }
                }

                con.commit();

                // Update cache
                existingProblem.setTitle(title);
                existingProblem.setDescription(statement);
                existingProblem.setTestCount(testCases.size());
                existingProblem.setSettings(settingsStr);
                existingProblem.setQualityScore(qualityScore);
                existingProblem.setCoverageSummary(coverageSummaryJson);
                projectService.saveProblemToRepositories(existingProblem);

            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", id);
        response.put("title", title);
        response.put("qualityScore", qualityScore);
        response.put("testCount", testCases.size());
        response.put("engineUsed", genResult.engineUsed());
        response.put("isFallback", genResult.isFallback());
        response.put("result", result);
        response.put("coverageSummary", root.get("coverageSummary"));
        response.put("requirements", root.get("requirements"));
        response.put("requirementsAnalyzed", root.get("requirementsAnalyzed"));
        response.put("potentialGaps", root.get("potentialGaps"));
        response.put("warnings", warningsList);
        response.put("qualityAudit", qualityAudit);

        return response;
    }

    @ExceptionHandler(InvalidProblemStatementException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidProblemStatement(InvalidProblemStatementException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getMessage());
        body.put("reason", e.getReason());
        body.put("suggestions", e.getSuggestions());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred."));
    }
}
