package com.testforge.backend.service.audit;

import com.testforge.backend.service.TestQualityService;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for test case static analysis rules.
 * Demonstrates inheritance and template evaluation in the quality audit engine.
 */
public abstract class AuditRule {

    protected final String ruleName;

    public AuditRule(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleName() {
        return ruleName;
    }

    /**
     * Polymorphic evaluation method executed across all registered rules.
     */
    public abstract void evaluate(
            List<Map<String, Object>> testCases,
            TestQualityService.QualityAuditResult audit,
            Map<Integer, List<String>> flagsByIndex
    );

    protected String getDisplayId(Map<String, Object> tc, int index) {
        Object id = tc.get("id");
        return id != null ? "#" + id : "Test Case " + (index + 1);
    }

    protected String normalizeString(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
