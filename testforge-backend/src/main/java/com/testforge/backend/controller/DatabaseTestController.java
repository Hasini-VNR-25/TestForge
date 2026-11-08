
package com.testforge.backend.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseTestController {

    @Value("${spring.datasource.url:jdbc:oracle:thin:@//localhost:1521/orcl}")
    private String dbUrl = "jdbc:oracle:thin:@//localhost:1521/orcl";

    @Value("${spring.datasource.username:SCOTT}")
    private String dbUser = "SCOTT";

    @Value("${spring.datasource.password:Oracle1234}")
    private String dbPassword = "Oracle1234";

    @GetMapping("/api/database-test")
    public Map<String, String> testDatabase() {
        String envPassword = System.getenv("ORACLE_DB_PASSWORD");
        String password = (envPassword != null && !envPassword.isBlank())
                ? envPassword
                : ((dbPassword != null && !dbPassword.isBlank()) ? dbPassword : "Oracle1234");
        String user = (dbUser != null && !dbUser.isBlank()) ? dbUser : "SCOTT";
        String url = (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : "jdbc:oracle:thin:@//localhost:1521/orcl";

        try (
            Connection connection = DriverManager.getConnection(
                url,
                user,
                password
            );
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(
                "SELECT COUNT(*) FROM TF_PROBLEMS"
            )
        ) {
            result.next();
            return Map.of(
                "status", "Connected to Oracle!",
                "problemCount", String.valueOf(result.getInt(1))
            );
        } catch (Exception e) {
            return Map.of(
                "status", "Database connection failed",
                "error", String.valueOf(e.getMessage())
            );
        }
    }
}
