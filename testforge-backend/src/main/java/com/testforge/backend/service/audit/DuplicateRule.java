package com.testforge.backend.service.audit;

import com.testforge.backend.service.TestQualityService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Concrete audit rule detecting exact duplicates using HashSet,
 * as well as near-duplicate inputs via Levenshtein edit distance.
 */
@Component
public class DuplicateRule extends AuditRule {

    public DuplicateRule() {
        super("Duplicate & Near-Duplicate Input Rule");
    }

    @Override
    public void evaluate(
            List<Map<String, Object>> testCases,
            TestQualityService.QualityAuditResult audit,
            Map<Integer, List<String>> flagsByIndex) {

        // 1. Exact Duplicate Detection using HashSet
        Set<String> seenInputs = new HashSet<>();
        Map<String, Integer> firstSeenIndex = new HashMap<>();

        for (int i = 0; i < testCases.size(); i++) {
            String input = normalizeString(String.valueOf(testCases.get(i).getOrDefault("input", "")));
            if (input.isBlank()) continue;

            if (seenInputs.contains(input)) {
                int originalIdx = firstSeenIndex.get(input);
                String origId = getDisplayId(testCases.get(originalIdx), originalIdx);
                String dupId = getDisplayId(testCases.get(i), i);

                flagsByIndex.get(i).add("Duplicate input of " + origId);
                audit.duplicateCount++;
                audit.issues.add("Duplicate detected: " + dupId + " has identical input to " + origId);
            } else {
                seenInputs.add(input);
                firstSeenIndex.put(input, i);
            }
        }

        // 2. Near-Duplicate Fuzzy Similarity Detection
        for (int i = 0; i < testCases.size(); i++) {
            String inputA = normalizeString(String.valueOf(testCases.get(i).getOrDefault("input", "")));
            if (inputA.length() < 4) continue;

            for (int j = i + 1; j < testCases.size(); j++) {
                String inputB = normalizeString(String.valueOf(testCases.get(j).getOrDefault("input", "")));
                if (inputB.length() < 4 || inputA.equals(inputB)) continue;

                double sim = calculateSimilarity(inputA, inputB);
                if (sim >= 0.88) {
                    String idA = getDisplayId(testCases.get(i), i);
                    String idB = getDisplayId(testCases.get(j), j);
                    flagsByIndex.get(j).add("High input similarity (" + Math.round(sim * 100) + "%) with " + idA);
                    audit.duplicateCount++;
                    audit.issues.add("Near duplicate: " + idB + " is " + Math.round(sim * 100) + "% similar to " + idA);
                }
            }
        }
    }

    private double calculateSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int dist = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) dist / maxLen);
    }

    private int computeLevenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
