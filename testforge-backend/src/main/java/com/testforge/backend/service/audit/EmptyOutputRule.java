package com.testforge.backend.service.audit;

import com.testforge.backend.service.TestQualityService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Concrete audit rule verifying data completeness (missing expected outputs,
 * blank inputs, or placeholder values).
 */
@Component
public class EmptyOutputRule extends AuditRule {

    public EmptyOutputRule() {
        super("Data Completeness & Placeholder Rule");
    }

    @Override
    public void evaluate(
            List<Map<String, Object>> testCases,
            TestQualityService.QualityAuditResult audit,
            Map<Integer, List<String>> flagsByIndex) {

        for (int i = 0; i < testCases.size(); i++) {
            Map<String, Object> tc = testCases.get(i);
            String input = String.valueOf(tc.getOrDefault("input", "")).trim();
            String output = String.valueOf(tc.getOrDefault("expectedOutput", "")).trim();
            String explanation = String.valueOf(tc.getOrDefault("explanation", "")).trim();
            String displayId = getDisplayId(tc, i);

            // Check missing or placeholder expected output
            if (output.isBlank() || output.equalsIgnoreCase("null") || output.equalsIgnoreCase("TODO") || output.equalsIgnoreCase("N/A")) {
                flagsByIndex.get(i).add("Missing or placeholder expected output");
                audit.flawCount++;
                audit.issues.add(displayId + ": Missing valid expected output.");
            }

            // Check missing input
            if (input.isBlank()) {
                flagsByIndex.get(i).add("Empty input data");
                audit.flawCount++;
                audit.issues.add(displayId + ": Input data is blank.");
            }

            // Check incomplete explanation
            if (explanation.length() < 8) {
                flagsByIndex.get(i).add("Incomplete or ultra-short explanation");
                audit.flawCount++;
            }
        }
    }
}
