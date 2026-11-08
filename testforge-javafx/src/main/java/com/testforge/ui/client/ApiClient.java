package com.testforge.ui.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.ui.model.AuditResultDto;
import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.model.TestCaseDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient client;
    private final ObjectMapper mapper;

    public ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public CompletableFuture<JsonNode> getDashboard() {
        return sendGet("/dashboard");
    }

    public CompletableFuture<List<ProblemDto>> getProjects() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/projects"))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    checkStatus(res);
                    try {
                        return mapper.readValue(res.body(), new TypeReference<List<ProblemDto>>() {});
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse projects list: " + e.getMessage(), e);
                    }
                });
    }

    public CompletableFuture<JsonNode> getProject(Long id) {
        return sendGet("/projects/" + id);
    }

    public CompletableFuture<Void> deleteProject(Long id) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/projects/" + id))
                .DELETE()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::checkStatus);
    }

    public CompletableFuture<JsonNode> generateTests(Map<String, Object> body) {
        return sendPost("/problems/generate-tests", body);
    }

    public CompletableFuture<Void> updateTestCase(Long id, TestCaseDto tc) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("problemId", tc.getProblemId());
        body.put("category", tc.getCategory());
        body.put("priority", tc.getPriority());
        body.put("requirementTag", tc.getRequirementTag());
        body.put("preconditions", tc.getPreconditions());
        body.put("input", tc.getInput());
        body.put("expectedOutput", tc.getExpectedOutput());
        body.put("explanation", tc.getExplanation());
        body.put("isReviewed", tc.getIsReviewed());

        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/test-cases/" + id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::checkStatus);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> toggleReview(Long id, boolean isReviewed) {
        Map<String, Object> body = Map.of("reviewed", isReviewed);
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/test-cases/" + id + "/review"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::checkStatus);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> deleteTestCase(Long id) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/test-cases/" + id))
                .DELETE()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::checkStatus);
    }

    public CompletableFuture<TestCaseDto> createTestCase(Long problemId, TestCaseDto tc) {
        try {
            String json = mapper.writeValueAsString(tc);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/problems/" + problemId + "/test-cases"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(res -> {
                        checkStatus(res);
                        try {
                            return mapper.readValue(res.body(), TestCaseDto.class);
                        } catch (Exception e) {
                            return tc;
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<AuditResultDto> auditQuality(List<TestCaseDto> cases) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            TestCaseDto tc = cases.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("id", tc.getId());
            m.put("input", tc.getInput());
            m.put("expectedOutput", tc.getExpectedOutput());
            m.put("explanation", tc.getExplanation());
            m.put("category", tc.getCategory());
            list.add(m);
        }

        try {
            String json = mapper.writeValueAsString(Map.of("testCases", list));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/quality/audit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(res -> {
                        checkStatus(res);
                        try {
                            return mapper.readValue(res.body(), AuditResultDto.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse audit result", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<JsonNode> getDatabaseStatus() {
        return sendGet("/database/status");
    }

    public CompletableFuture<JsonNode> getDatabaseTables() {
        return sendGet("/database/tables");
    }

    public CompletableFuture<JsonNode> getTableData(String tableName) {
        return sendGet("/database/table/" + tableName);
    }

    public CompletableFuture<JsonNode> runSqlQuery(String sql) {
        return sendPost("/database/query", Map.of("query", sql));
    }

    private CompletableFuture<JsonNode> sendGet(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    checkStatus(res);
                    try {
                        return mapper.readTree(res.body());
                    } catch (Exception e) {
                        throw new RuntimeException("JSON Parse error: " + e.getMessage(), e);
                    }
                });
    }

    private CompletableFuture<JsonNode> sendPost(String path, Object body) {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(res -> {
                        checkStatus(res);
                        try {
                            return mapper.readTree(res.body());
                        } catch (Exception e) {
                            throw new RuntimeException("JSON Parse error: " + e.getMessage(), e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<JsonNode> regenerateProject(Long problemId, Map<String, Object> reqBody) {
        try {
            String json = mapper.writeValueAsString(reqBody != null ? reqBody : Collections.emptyMap());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/problems/" + problemId + "/regenerate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(res -> {
                        checkStatus(res);
                        try {
                            return mapper.readTree(res.body());
                        } catch (Exception e) {
                            throw new RuntimeException("JSON Parse error: " + e.getMessage(), e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void checkStatus(HttpResponse<?> res) {
        if (res.statusCode() >= 400) {
            String body = res.body() != null ? res.body().toString() : "";
            try {
                JsonNode root = mapper.readTree(body);
                if (root.has("reason") || root.has("suggestions")) {
                    StringBuilder sb = new StringBuilder();
                    String reason = root.path("reason").asText("");
                    if (!reason.isBlank()) {
                        sb.append(reason);
                    } else if (root.has("error")) {
                        sb.append(root.path("error").asText());
                    }

                    if (root.has("suggestions") && root.path("suggestions").isArray() && !root.path("suggestions").isEmpty()) {
                        sb.append("\n\nSuggestions:\n");
                        for (JsonNode sug : root.path("suggestions")) {
                            sb.append("• ").append(sug.asText()).append("\n");
                        }
                    }
                    throw new RuntimeException(sb.toString().trim());
                } else if (root.has("error")) {
                    throw new RuntimeException(root.path("error").asText());
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ignored) {
            }
            throw new RuntimeException("API Error (" + res.statusCode() + "): " + body);
        }
    }
}
