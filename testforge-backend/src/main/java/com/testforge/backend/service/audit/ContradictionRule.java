package com.testforge.backend.service.audit;

import com.testforge.backend.service.TestQualityService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Concrete audit rule detecting logical contradictions:
 * identical inputs mapped to conflicting expected outputs.
 */
@Component
public class ContradictionRule extends AuditRule {

    public ContradictionRule() {
        super("Logical Contradiction Rule");
    }

    @Override
    public void evaluate(
            List<Map<String, Object>> testCases,
            TestQualityService.QualityAuditResult audit,
            Map<Integer, List<String>> flagsByIndex) {

        Map<String, List<Integer>> inputToIndices = new HashMap<>();

        for (int i = 0; i < testCases.size(); i++) {
            String input = normalizeString(String.valueOf(testCases.get(i).getOrDefault("input", "")));
            if (!input.isBlank()) {
                inputToIndices.computeIfAbsent(input, k -> new ArrayList<>()).add(i);
            }
        }

        for (Map.Entry<String, List<Integer>> entry : inputToIndices.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() > 1) {
                for (int m = 0; m < indices.size(); m++) {
                    for (int n = m + 1; n < indices.size(); n++) {
                        int idx1 = indices.get(m);
                        int idx2 = indices.get(n);
                        String out1 = normalizeString(String.valueOf(testCases.get(idx1).getOrDefault("expectedOutput", "")));
                        String out2 = normalizeString(String.valueOf(testCases.get(idx2).getOrDefault("expectedOutput", "")));

                        String id1 = getDisplayId(testCases.get(idx1), idx1);
                        String id2 = getDisplayId(testCases.get(idx2), idx2);

                        if (!out1.equals(out2) && !out1.isBlank() && !out2.isBlank()) {
                            flagsByIndex.get(idx1).add("Contradiction with " + id2 + " (same input, conflicting output)");
                            flagsByIndex.get(idx2).add("Contradiction with " + id1 + " (same input, conflicting output)");
                            audit.contradictionCount++;
                            audit.issues.add("Critical contradiction: " + id1 + " and " + id2 + " have identical input but conflicting expected outputs!");
                        }
                    }
                }
            }
        }
    }
}
