package com.testforge.backend.service;

import com.testforge.backend.config.ConfigLoader;
import com.testforge.backend.model.Problem;
import com.testforge.backend.model.TestCase;
import com.testforge.backend.repository.InMemoryRepository;
import com.testforge.backend.repository.JsonFileRepository;
import com.testforge.backend.repository.Repository;
import com.testforge.backend.util.CollectionsHelper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

@Service
public class ProjectService {

    @Value("${spring.datasource.url:jdbc:oracle:thin:@//localhost:1521/orcl}")
    private String dbUrl = "jdbc:oracle:thin:@//localhost:1521/orcl";

    @Value("${spring.datasource.username:SCOTT}")
    private String dbUser = "SCOTT";

    @Value("${spring.datasource.password:Oracle1234}")
    private String dbPassword = "Oracle1234";

    private final Repository<Problem, Long> inMemoryRepo = new InMemoryRepository<>(Problem::getId);
    private final Repository<Problem, Long> jsonFileRepo;

    public ProjectService() {
        String backupDir = ConfigLoader.getInstance().getStringProperty("testforge.storage.backup-dir", "./backups");
        Path backupPath = Paths.get(backupDir, "problems.json");
        this.jsonFileRepo = new JsonFileRepository<>(backupPath, Problem.class, Problem::getId);
    }

    @PostConstruct
    public void initRepositorySync() {
        try {
            List<Problem> dbProjects = getAllProjectsFromDb();
            for (Problem p : dbProjects) {
                inMemoryRepo.save(p);
                jsonFileRepo.save(p);
            }
        } catch (Exception e) {
            // If Oracle is offline, populate cache from JSON persistence repository
            for (Problem p : jsonFileRepo.findAll()) {
                inMemoryRepo.save(p);
            }
        }
    }

    public Repository<Problem, Long> getInMemoryRepo() {
        return inMemoryRepo;
    }

    public Repository<Problem, Long> getJsonFileRepo() {
        return jsonFileRepo;
    }

    public void saveProblemToRepositories(Problem problem) {
        if (problem != null) {
            inMemoryRepo.save(problem);
            jsonFileRepo.save(problem);
        }
    }

    public Connection getConnection() throws SQLException {
        String envPassword = System.getenv("ORACLE_DB_PASSWORD");
        String password = (envPassword != null && !envPassword.isBlank())
                ? envPassword
                : ((dbPassword != null && !dbPassword.isBlank()) ? dbPassword : "Oracle1234");
        String user = (dbUser != null && !dbUser.isBlank()) ? dbUser : "SCOTT";
        String url = (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : "jdbc:oracle:thin:@//localhost:1521/orcl";

        return DriverManager.getConnection(url, user, password);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try (Connection con = getConnection()) {
            // Total problems
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM TF_PROBLEMS")) {
                if (rs.next()) {
                    stats.put("totalProblems", rs.getLong(1));
                }
            }

            // Total test cases & reviewed count
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*), NVL(SUM(CASE WHEN IS_REVIEWED = 1 THEN 1 ELSE 0 END), 0) FROM TF_TEST_CASES")) {
                if (rs.next()) {
                    stats.put("totalTestCases", rs.getLong(1));
                    stats.put("reviewedTestCases", rs.getLong(2));
                }
            }

            // Average quality score
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT NVL(ROUND(AVG(QUALITY_SCORE), 1), 0) FROM TF_PROBLEMS")) {
                if (rs.next()) {
                    stats.put("averageQualityScore", rs.getDouble(1));
                }
            }

            // Category distribution computed using EnumMap
            EnumMap<com.testforge.backend.model.Category, Long> categoryEnumMap = new EnumMap<>(com.testforge.backend.model.Category.class);
            for (com.testforge.backend.model.Category c : com.testforge.backend.model.Category.values()) {
                categoryEnumMap.put(c, 0L);
            }

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT CATEGORY, COUNT(*) FROM TF_TEST_CASES GROUP BY CATEGORY")) {
                while (rs.next()) {
                    String catStr = rs.getString(1);
                    if (catStr != null) {
                        com.testforge.backend.model.Category c = com.testforge.backend.model.Category.from(catStr);
                        categoryEnumMap.put(c, categoryEnumMap.getOrDefault(c, 0L) + rs.getLong(2));
                    }
                }
            }

            Map<String, Long> categoryCounts = new LinkedHashMap<>();
            categoryEnumMap.forEach((cat, count) -> categoryCounts.put(cat.name(), count));
            stats.put("categoryCounts", categoryCounts);

            // Recent 5 projects
            List<Map<String, Object>> recentProjects = new ArrayList<>();
            String recentSql = "SELECT PROBLEM_ID, TITLE, CREATED_AT, TEST_COUNT, QUALITY_SCORE " +
                               "FROM (SELECT * FROM TF_PROBLEMS ORDER BY PROBLEM_ID DESC) WHERE ROWNUM <= 5";

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(recentSql)) {
                while (rs.next()) {
                    Map<String, Object> proj = new HashMap<>();
                    proj.put("id", rs.getLong("PROBLEM_ID"));
                    proj.put("title", rs.getString("TITLE"));
                    proj.put("createdAt", rs.getTimestamp("CREATED_AT"));
                    proj.put("testCount", rs.getInt("TEST_COUNT"));
                    proj.put("qualityScore", rs.getInt("QUALITY_SCORE"));
                    recentProjects.add(proj);
                }
            }
            // Compute Top Quality Score using bounded generic CollectionsHelper.findMax
            List<Problem> allProjects = getAllProjects();
            List<Integer> scores = allProjects.stream()
                    .map(Problem::getQualityScore)
                    .filter(Objects::nonNull)
                    .toList();
            int topQualityScore = CollectionsHelper.findMax(scores).orElse(0);
            stats.put("topQualityScore", topQualityScore);

            // Compute Top 5 Ranked Projects using bounded generic CollectionsHelper.sortTop
            List<Problem> topProjects = CollectionsHelper.sortTop(allProjects, 5);
            stats.put("topProjects", topProjects);

            stats.put("recentProjects", recentProjects);

        } catch (Exception e) {
            stats.put("error", e.getMessage());

            // Resilient fallback powered by InMemoryRepository, JsonFileRepository, and CollectionsHelper
            List<Problem> cached = inMemoryRepo.findAll();
            if (cached.isEmpty()) {
                cached = jsonFileRepo.findAll();
            }
            stats.put("totalProblems", (long) cached.size());
            stats.put("totalTestCases", cached.stream().mapToLong(p -> p.getTestCount() != null ? p.getTestCount() : 0).sum());
            stats.put("reviewedTestCases", 0L);
            double avg = cached.isEmpty() ? 0.0 : Math.round(cached.stream().mapToInt(p -> p.getQualityScore() != null ? p.getQualityScore() : 0).average().orElse(0.0) * 10.0) / 10.0;
            stats.put("averageQualityScore", avg);

            List<Integer> fallbackScores = cached.stream().map(Problem::getQualityScore).filter(Objects::nonNull).toList();
            stats.put("topQualityScore", CollectionsHelper.findMax(fallbackScores).orElse(0));
            stats.put("topProjects", CollectionsHelper.sortTop(cached, 5));

            Map<String, Long> categoryCounts = new LinkedHashMap<>();
            for (com.testforge.backend.model.Category c : com.testforge.backend.model.Category.values()) {
                categoryCounts.put(c.name(), 0L);
            }
            stats.put("categoryCounts", categoryCounts);

            List<Map<String, Object>> fallbackRecent = new ArrayList<>();
            for (Problem p : cached.stream().limit(5).toList()) {
                Map<String, Object> proj = new HashMap<>();
                proj.put("id", p.getId());
                proj.put("title", p.getTitle());
                proj.put("createdAt", p.getCreatedAt());
                proj.put("testCount", p.getTestCount());
                proj.put("qualityScore", p.getQualityScore());
                fallbackRecent.add(proj);
            }
            stats.put("recentProjects", fallbackRecent);
        }

        return stats;
    }

    public List<Problem> getAllProjects() {
        try {
            List<Problem> list = getAllProjectsFromDb();
            // Cache and backup into repositories
            for (Problem p : list) {
                inMemoryRepo.save(p);
                jsonFileRepo.save(p);
            }
            return list;
        } catch (Exception e) {
            // Graceful fallback to repository cache if database is down
            List<Problem> fallback = inMemoryRepo.findAll();
            if (fallback.isEmpty()) {
                fallback = jsonFileRepo.findAll();
            }
            return fallback;
        }
    }

    public List<Problem> getAllProjectsFromDb() throws SQLException {
        List<Problem> list = new ArrayList<>();
        String sql = "SELECT PROBLEM_ID, TITLE, DESCRIPTION, CREATED_AT, TEST_COUNT, SETTINGS, QUALITY_SCORE, COVERAGE_SUMMARY " +
                     "FROM TF_PROBLEMS ORDER BY PROBLEM_ID DESC";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Problem p = new Problem();
                p.setId(rs.getLong("PROBLEM_ID"));
                p.setTitle(rs.getString("TITLE"));
                p.setDescription(rs.getString("DESCRIPTION"));
                p.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                p.setTestCount(rs.getInt("TEST_COUNT"));
                p.setSettings(rs.getString("SETTINGS"));
                p.setQualityScore(rs.getInt("QUALITY_SCORE"));
                p.setCoverageSummary(rs.getString("COVERAGE_SUMMARY"));
                list.add(p);
            }
        }
        return list;
    }

    public Map<String, Object> getProjectDetails(Long problemId) {
        Map<String, Object> response = new HashMap<>();

        try (Connection con = getConnection()) {
            Problem p = null;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT PROBLEM_ID, TITLE, DESCRIPTION, CREATED_AT, TEST_COUNT, SETTINGS, QUALITY_SCORE, COVERAGE_SUMMARY " +
                    "FROM TF_PROBLEMS WHERE PROBLEM_ID = ?")) {
                ps.setLong(1, problemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        p = new Problem();
                        p.setId(rs.getLong("PROBLEM_ID"));
                        p.setTitle(rs.getString("TITLE"));
                        p.setDescription(rs.getString("DESCRIPTION"));
                        p.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                        p.setTestCount(rs.getInt("TEST_COUNT"));
                        p.setSettings(rs.getString("SETTINGS"));
                        p.setQualityScore(rs.getInt("QUALITY_SCORE"));
                        p.setCoverageSummary(rs.getString("COVERAGE_SUMMARY"));
                    }
                }
            }

            if (p == null) {
                p = inMemoryRepo.findById(problemId).or(() -> jsonFileRepo.findById(problemId)).orElse(null);
            }

            if (p == null) {
                throw new NoSuchElementException("Project not found: " + problemId);
            }
            response.put("project", p);

            List<TestCase> testCases = getTestCasesByProblem(con, problemId);
            response.put("testCases", testCases);

        } catch (Exception e) {
            Optional<Problem> cached = inMemoryRepo.findById(problemId).or(() -> jsonFileRepo.findById(problemId));
            if (cached.isPresent()) {
                response.put("project", cached.get());
                response.put("testCases", Collections.emptyList());
                return response;
            }
            throw new RuntimeException("Error loading project " + problemId + ": " + e.getMessage(), e);
        }

        return response;
    }

    public List<TestCase> getTestCasesByProblem(Connection con, Long problemId) throws SQLException {
        List<TestCase> list = new ArrayList<>();
        String sql = "SELECT TEST_CASE_ID, PROBLEM_ID, INPUT_DATA, EXPECTED_OUTPUT, CATEGORY, EXPLANATION, " +
                     "PRIORITY, PRECONDITIONS, IS_REVIEWED, REQUIREMENT_TAG " +
                     "FROM TF_TEST_CASES WHERE PROBLEM_ID = ? ORDER BY TEST_CASE_ID ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, problemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TestCase tc = new TestCase();
                    tc.setId(rs.getLong("TEST_CASE_ID"));
                    tc.setProblemId(rs.getLong("PROBLEM_ID"));
                    tc.setInput(rs.getString("INPUT_DATA"));
                    tc.setExpectedOutput(rs.getString("EXPECTED_OUTPUT"));
                    tc.setCategory(rs.getString("CATEGORY"));
                    tc.setExplanation(rs.getString("EXPLANATION"));
                    tc.setPriority(rs.getString("PRIORITY"));
                    tc.setPreconditions(rs.getString("PRECONDITIONS"));
                    tc.setIsReviewed(rs.getInt("IS_REVIEWED") == 1);
                    tc.setRequirementTag(rs.getString("REQUIREMENT_TAG"));
                    list.add(tc);
                }
            }
        }
        return list;
    }

    public List<TestCase> getTestCasesByProblem(Long problemId) {
        try (Connection con = getConnection()) {
            return getTestCasesByProblem(con, problemId);
        } catch (Exception e) {
            throw new RuntimeException("Error loading test cases: " + e.getMessage(), e);
        }
    }

    public void deleteProject(Long problemId) {
        // Remove from in-memory cache and JSON persistence repositories
        inMemoryRepo.deleteById(problemId);
        jsonFileRepo.deleteById(problemId);

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM TF_TEST_CASES WHERE PROBLEM_ID = ?")) {
                    ps.setLong(1, problemId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM TF_PROBLEMS WHERE PROBLEM_ID = ?")) {
                    ps.setLong(1, problemId);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting project: " + e.getMessage(), e);
        }
    }

    public boolean updateTestCase(Long testCaseId, TestCase tc) {
        String sql = "UPDATE TF_TEST_CASES SET INPUT_DATA = ?, EXPECTED_OUTPUT = ?, CATEGORY = ?, " +
                     "EXPLANATION = ?, PRIORITY = ?, PRECONDITIONS = ?, IS_REVIEWED = ?, REQUIREMENT_TAG = ? " +
                     "WHERE TEST_CASE_ID = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tc.getInput() != null ? tc.getInput() : "");
            ps.setString(2, tc.getExpectedOutput() != null ? tc.getExpectedOutput() : "");
            ps.setString(3, tc.getCategory() != null ? tc.getCategory() : "NORMAL");
            ps.setString(4, tc.getExplanation() != null ? tc.getExplanation() : "");
            ps.setString(5, tc.getPriority() != null ? tc.getPriority() : "MEDIUM");
            ps.setString(6, tc.getPreconditions() != null ? tc.getPreconditions() : "");
            ps.setInt(7, (tc.getIsReviewed() != null && tc.getIsReviewed()) ? 1 : 0);
            ps.setString(8, tc.getRequirementTag() != null ? tc.getRequirementTag() : "");
            ps.setLong(9, testCaseId);

            int rows = ps.executeUpdate();
            if (!con.getAutoCommit()) {
                con.commit();
            }
            return rows > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error updating test case: " + e.getMessage(), e);
        }
    }

    public boolean toggleReviewStatus(Long testCaseId, boolean isReviewed) {
        String sql = "UPDATE TF_TEST_CASES SET IS_REVIEWED = ? WHERE TEST_CASE_ID = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, isReviewed ? 1 : 0);
            ps.setLong(2, testCaseId);
            int rows = ps.executeUpdate();
            if (!con.getAutoCommit()) {
                con.commit();
            }
            return rows > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error toggling review status: " + e.getMessage(), e);
        }
    }

    public boolean deleteTestCase(Long testCaseId) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                Long problemId = null;
                try (PreparedStatement ps = con.prepareStatement("SELECT PROBLEM_ID FROM TF_TEST_CASES WHERE TEST_CASE_ID = ?")) {
                    ps.setLong(1, testCaseId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            problemId = rs.getLong(1);
                        }
                    }
                }

                if (problemId == null) {
                    return false;
                }

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM TF_TEST_CASES WHERE TEST_CASE_ID = ?")) {
                    ps.setLong(1, testCaseId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE TF_PROBLEMS SET TEST_COUNT = (SELECT COUNT(*) FROM TF_TEST_CASES WHERE PROBLEM_ID = ?) WHERE PROBLEM_ID = ?")) {
                    ps.setLong(1, problemId);
                    ps.setLong(2, problemId);
                    ps.executeUpdate();
                }

                con.commit();

                // Update in-memory and file repository backup cache
                final Long pid = problemId;
                inMemoryRepo.findById(pid).ifPresent(p -> {
                    int c = p.getTestCount() != null ? Math.max(0, p.getTestCount() - 1) : 0;
                    p.setTestCount(c);
                    inMemoryRepo.save(p);
                    jsonFileRepo.save(p);
                });

                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting test case: " + e.getMessage(), e);
        }
    }

    public TestCase createTestCase(Long problemId, TestCase tc) {
        String sql = "INSERT INTO TF_TEST_CASES (TEST_CASE_ID, PROBLEM_ID, INPUT_DATA, EXPECTED_OUTPUT, CATEGORY, EXPLANATION, PRIORITY, PRECONDITIONS, IS_REVIEWED, REQUIREMENT_TAG) " +
                     "VALUES (TF_TEST_CASE_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                long generatedId = 0;
                try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"TEST_CASE_ID"})) {
                    ps.setLong(1, problemId);
                    ps.setString(2, tc.getInput());
                    ps.setString(3, tc.getExpectedOutput());
                    ps.setString(4, tc.getCategory());
                    ps.setString(5, tc.getExplanation());
                    ps.setString(6, tc.getPriority());
                    ps.setString(7, tc.getPreconditions() != null ? tc.getPreconditions() : "Standard input");
                    ps.setInt(8, (tc.getIsReviewed() != null && tc.getIsReviewed()) ? 1 : 0);
                    ps.setString(9, tc.getRequirementTag());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getLong(1);
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE TF_PROBLEMS SET TEST_COUNT = (SELECT COUNT(*) FROM TF_TEST_CASES WHERE PROBLEM_ID = ?) WHERE PROBLEM_ID = ?")) {
                    ps.setLong(1, problemId);
                    ps.setLong(2, problemId);
                    ps.executeUpdate();
                }

                con.commit();
                tc.setId(generatedId);
                tc.setProblemId(problemId);
                return tc;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating test case: " + e.getMessage(), e);
        }
    }
}
