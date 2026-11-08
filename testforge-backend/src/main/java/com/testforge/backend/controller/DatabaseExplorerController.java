package com.testforge.backend.controller;

import com.testforge.backend.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DatabaseExplorerController {

    private final ProjectService projectService;

    public DatabaseExplorerController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> resp = new HashMap<>();
        try (Connection con = projectService.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            resp.put("connected", true);
            resp.put("databaseProduct", meta.getDatabaseProductName());
            resp.put("databaseVersion", meta.getDatabaseProductVersion());
            resp.put("userName", meta.getUserName());
            resp.put("url", meta.getURL());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("connected", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<List<Map<String, Object>>> getTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection con = projectService.getConnection()) {
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME")) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    long rowCount = 0;
                    try (Statement countSt = con.createStatement();
                         ResultSet countRs = countSt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
                        if (countRs.next()) {
                            rowCount = countRs.getLong(1);
                        }
                    } catch (Exception ignored) {}

                    Map<String, Object> t = new HashMap<>();
                    t.put("tableName", tableName);
                    t.put("rowCount", rowCount);
                    tables.add(t);
                }
            }
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @GetMapping("/table/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableData(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {

        tableName = tableName.toUpperCase().trim();
        Map<String, Object> result = new HashMap<>();

        try (Connection con = projectService.getConnection()) {
            // Verify table belongs to current user
            try (PreparedStatement chk = con.prepareStatement("SELECT 1 FROM USER_TABLES WHERE TABLE_NAME = ?")) {
                chk.setString(1, tableName);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Table not found: " + tableName));
                    }
                }
            }

            // Columns metadata
            List<Map<String, String>> columns = new ArrayList<>();
            try (PreparedStatement colSt = con.prepareStatement(
                    "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID")) {
                colSt.setString(1, tableName);
                try (ResultSet rs = colSt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> col = new HashMap<>();
                        col.put("name", rs.getString("COLUMN_NAME"));
                        col.put("type", rs.getString("DATA_TYPE"));
                        col.put("length", rs.getString("DATA_LENGTH"));
                        col.put("nullable", rs.getString("NULLABLE"));
                        columns.add(col);
                    }
                }
            }
            result.put("columns", columns);

            // Fetch table rows
            List<Map<String, Object>> rows = new ArrayList<>();
            String query = "SELECT * FROM \"" + tableName + "\" WHERE ROWNUM <= " + Math.min(Math.max(limit, 1), 500);
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = md.getColumnName(i);
                        String val = rs.getString(i);
                        row.put(colName, val);
                    }
                    rows.add(row);
                }
            }
            result.put("rows", rows);
            result.put("tableName", tableName);
            result.put("totalFetched", rows.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> runQuery(@RequestBody Map<String, String> body) {
        String sql = body.get("query");
        if (sql == null || sql.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query cannot be empty"));
        }

        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT") && !trimmed.startsWith("WITH")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only read-only SELECT queries are permitted in DB Explorer for database integrity."));
        }

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection con = projectService.getConnection();
             Statement st = con.createStatement()) {
            st.setMaxRows(300);
            try (ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                List<String> colNames = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    colNames.add(md.getColumnName(i));
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(md.getColumnName(i), rs.getString(i));
                    }
                    rows.add(row);
                }

                long durationMs = System.currentTimeMillis() - startTime;
                response.put("columns", colNames);
                response.put("rows", rows);
                response.put("rowCount", rows.size());
                response.put("executionTimeMs", durationMs);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
