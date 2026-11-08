package com.testforge.backend.service;

import com.testforge.backend.service.audit.AuditRule;
import com.testforge.backend.service.audit.ContradictionRule;
import com.testforge.backend.service.audit.DuplicateRule;
import com.testforge.backend.service.audit.EmptyOutputRule;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Quality analysis engine orchestrating modular AuditRule evaluations.
 * Leverages inheritance and polymorphism across individual static analysis rules.
 */
@Service
public class TestQualityService {

    public static class QualityAuditResult {
        public int totalCases;
        public int duplicateCount;
        public int contradictionCount;
        public int flawCount;
        public String healthBadge; // "EXCELLENT", "GOOD", "WARNING", "CRITICAL"
        public List<String> issues = new ArrayList<>();
        public List<Map<String, Object>> caseFlags = new ArrayList<>();
    }

    private final List<AuditRule> auditRules;

    public TestQualityService(List<AuditRule> auditRules) {
        this.auditRules = (auditRules != null && !auditRules.isEmpty())
                ? auditRules
                : List.of(new EmptyOutputRule(), new ContradictionRule(), new DuplicateRule());
    }

    public QualityAuditResult auditTestCases(List<Map<String, Object>> testCases) {
        QualityAuditResult audit = new QualityAuditResult();
        audit.totalCases = testCases != null ? testCases.size() : 0;
        if (testCases == null || testCases.isEmpty()) {
            audit.healthBadge = "EMPTY";
            return audit;
        }

        Map<Integer, List<String>> flagsByIndex = new HashMap<>();
        for (int i = 0; i < testCases.size(); i++) {
            flagsByIndex.put(i, new ArrayList<>());
        }

        // Polymorphic rule evaluation across all registered AuditRules
        for (AuditRule rule : auditRules) {
            rule.evaluate(testCases, audit, flagsByIndex);
        }

        // Format case flags for UI rendering
        for (int i = 0; i < testCases.size(); i++) {
            Map<String, Object> flagMap = new HashMap<>();
            flagMap.put("index", i);
            flagMap.put("id", testCases.get(i).get("id"));
            flagMap.put("flags", flagsByIndex.get(i));
            flagMap.put("hasIssues", !flagsByIndex.get(i).isEmpty());
            audit.caseFlags.add(flagMap);
        }

        // Assign Health Badge based on rule severity findings
        if (audit.contradictionCount > 0) {
            audit.healthBadge = "CRITICAL";
        } else if (audit.duplicateCount > 0 || audit.flawCount > 2) {
            audit.healthBadge = "WARNING";
        } else if (audit.flawCount > 0) {
            audit.healthBadge = "GOOD";
        } else {
            audit.healthBadge = "EXCELLENT";
        }

        return audit;
    }
}
