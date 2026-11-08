package com.testforge.backend.controller;

import com.testforge.backend.model.TestCase;
import com.testforge.backend.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class TestCaseController {

    private final ProjectService projectService;

    public TestCaseController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/problems/{problemId}/test-cases")
    public List<TestCase> getTestCases(@PathVariable("problemId") Long problemId) {
        return projectService.getTestCasesByProblem(problemId);
    }

    @PostMapping("/problems/{problemId}/test-cases")
    public TestCase createTestCase(@PathVariable("problemId") Long problemId, @RequestBody TestCase testCase) {
        return projectService.createTestCase(problemId, testCase);
    }

    @PutMapping("/test-cases/{id}")
    public Map<String, Object> updateTestCase(
            @PathVariable("id") Long id,
            @RequestBody TestCase testCase) {
        boolean updated = projectService.updateTestCase(id, testCase);
        if (!updated) {
            throw new RuntimeException("Test case not found with ID: " + id);
        }
        return Map.of("success", true, "message", "Test case updated successfully");
    }

    @PatchMapping("/test-cases/{id}/review")
    public Map<String, Object> toggleReview(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> payload) {
        Object val = payload != null ? payload.get("reviewed") : null;
        if (val == null && payload != null) {
            val = payload.get("isReviewed");
        }
        boolean reviewed = true;
        if (val instanceof Boolean b) {
            reviewed = b;
        } else if (val instanceof Number n) {
            reviewed = n.intValue() == 1;
        } else if (val instanceof String s) {
            reviewed = Boolean.parseBoolean(s) || "1".equals(s);
        }
        boolean updated = projectService.toggleReviewStatus(id, reviewed);
        if (!updated) {
            throw new RuntimeException("Test case not found with ID: " + id);
        }
        return Map.of("success", true, "isReviewed", reviewed);
    }

    @DeleteMapping("/test-cases/{id}")
    public Map<String, Object> deleteTestCase(@PathVariable("id") Long id) {
        boolean deleted = projectService.deleteTestCase(id);
        if (!deleted) {
            throw new RuntimeException("Test case not found with ID: " + id);
        }
        return Map.of("success", true, "message", "Test case deleted successfully");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred."));
    }
}
