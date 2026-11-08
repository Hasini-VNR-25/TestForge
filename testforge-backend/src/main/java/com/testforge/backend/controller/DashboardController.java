package com.testforge.backend.controller;

import com.testforge.backend.config.ConfigLoader;
import com.testforge.backend.model.Problem;
import com.testforge.backend.service.ProjectService;
import com.testforge.backend.service.TestQualityService;
import com.testforge.backend.util.CollectionsHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final ProjectService projectService;
    private final TestQualityService qualityService;

    public DashboardController(ProjectService projectService, TestQualityService qualityService) {
        this.projectService = projectService;
        this.qualityService = qualityService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return projectService.getDashboardStats();
    }

    @PostMapping("/quality/audit")
    public ResponseEntity<?> auditTestCases(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> testCases = new ArrayList<>();
        Object tcList = request.get("testCases");

        if (tcList instanceof List<?>) {
            for (Object obj : (List<?>) tcList) {
                if (obj instanceof Map<?, ?> map) {
                    Map<String, Object> tc = new HashMap<>();
                    map.forEach((k, v) -> tc.put(String.valueOf(k), v));
                    testCases.add(tc);
                }
            }
        }

        TestQualityService.QualityAuditResult result = qualityService.auditTestCases(testCases);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/config")
    public Map<String, String> getConfiguration() {
        return ConfigLoader.getInstance().getAllProperties();
    }

    @GetMapping("/projects/top")
    public List<Problem> getTopProjects(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        return CollectionsHelper.sortTop(projectService.getAllProjects(), limit);
    }

    @GetMapping("/projects")
    public List<Problem> getProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/projects/{id}")
    public Map<String, Object> getProjectDetails(@PathVariable("id") Long id) {
        return projectService.getProjectDetails(id);
    }

    @DeleteMapping("/projects/{id}")
    public Map<String, String> deleteProject(@PathVariable("id") Long id) {
        projectService.deleteProject(id);
        return Map.of("message", "Project and associated test cases deleted successfully");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred."));
    }
}
