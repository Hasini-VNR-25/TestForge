package com.testforge.backend.controller;

import com.testforge.backend.model.Problem;
import com.testforge.backend.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProjectService projectService;

    public ProblemController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<Problem> getAllProblems() {
        return projectService.getAllProjects();
    }

    @GetMapping("/backup")
    public List<Problem> getBackupProblems() {
        return projectService.getJsonFileRepo().findAll();
    }

    @GetMapping("/cache")
    public List<Problem> getCachedProblems() {
        return projectService.getInMemoryRepo().findAll();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProblem(@PathVariable("id") Long id) {
        return projectService.getProjectDetails(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteProblem(@PathVariable("id") Long id) {
        projectService.deleteProject(id);
        return Map.of("message", "Problem deleted successfully");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred."));
    }
}
