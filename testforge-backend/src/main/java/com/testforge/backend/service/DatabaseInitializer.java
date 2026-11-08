package com.testforge.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DatabaseInitializer {

    @Value("${spring.datasource.url:jdbc:oracle:thin:@//localhost:1521/orcl}")
    private String dbUrl = "jdbc:oracle:thin:@//localhost:1521/orcl";

    @Value("${spring.datasource.username:SCOTT}")
    private String dbUser = "SCOTT";

    @Value("${spring.datasource.password:Oracle1234}")
    private String dbPassword = "Oracle1234";

    @PostConstruct
    public void init() {
        String envPassword = System.getenv("ORACLE_DB_PASSWORD");
        String password = (envPassword != null && !envPassword.isBlank())
                ? envPassword
                : ((dbPassword != null && !dbPassword.isBlank()) ? dbPassword : "Oracle1234");
        String user = (dbUser != null && !dbUser.isBlank()) ? dbUser : "SCOTT";
        String url = (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : "jdbc:oracle:thin:@//localhost:1521/orcl";

        String[] ddlStatements = {
            "ALTER TABLE TF_PROBLEMS ADD (CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "ALTER TABLE TF_PROBLEMS ADD (TEST_COUNT NUMBER DEFAULT 0)",
            "ALTER TABLE TF_PROBLEMS ADD (SETTINGS VARCHAR2(500))",
            "ALTER TABLE TF_PROBLEMS ADD (QUALITY_SCORE NUMBER DEFAULT 0)",
            "ALTER TABLE TF_PROBLEMS ADD (COVERAGE_SUMMARY CLOB)",
            "ALTER TABLE TF_TEST_CASES ADD (PRIORITY VARCHAR2(20) DEFAULT 'MEDIUM')",
            "ALTER TABLE TF_TEST_CASES ADD (PRECONDITIONS CLOB)",
            "ALTER TABLE TF_TEST_CASES ADD (IS_REVIEWED NUMBER(1) DEFAULT 0)",
            "ALTER TABLE TF_TEST_CASES ADD (REQUIREMENT_TAG VARCHAR2(100))"
        };

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement()) {
            for (String sql : ddlStatements) {
                try {
                    st.execute(sql);
                } catch (SQLException e) {
                    // ORA-01430: column being added already exists in table
                    if (e.getErrorCode() != 1430) {
                        System.err.println("DatabaseInitializer notice on: " + sql + " -> " + e.getMessage());
                    }
                }
            }
            // Normalize any legacy NULL IS_REVIEWED records to 0
            try {
                st.executeUpdate("UPDATE TF_TEST_CASES SET IS_REVIEWED = 0 WHERE IS_REVIEWED IS NULL");
                if (!con.getAutoCommit()) {
                    con.commit();
                }
            } catch (SQLException e) {
                System.err.println("DatabaseInitializer notice on IS_REVIEWED normalization: " + e.getMessage());
            }
            System.out.println("TestForge database schema initialized successfully.");
        } catch (Exception e) {
            System.err.println("DatabaseInitializer could not connect to Oracle: " + e.getMessage());
        }
    }
}
