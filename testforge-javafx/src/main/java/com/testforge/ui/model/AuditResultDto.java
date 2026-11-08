package com.testforge.ui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditResultDto {
    private int totalCases;
    private int duplicateCount;
    private int contradictionCount;
    private int flawCount;
    private String healthBadge = "EXCELLENT";
    private List<String> issues = new ArrayList<>();
    private List<Map<String, Object>> caseFlags = new ArrayList<>();

    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }

    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }

    public int getContradictionCount() { return contradictionCount; }
    public void setContradictionCount(int contradictionCount) { this.contradictionCount = contradictionCount; }

    public int getFlawCount() { return flawCount; }
    public void setFlawCount(int flawCount) { this.flawCount = flawCount; }

    public String getHealthBadge() { return healthBadge; }
    public void setHealthBadge(String healthBadge) { this.healthBadge = healthBadge; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }

    public List<Map<String, Object>> getCaseFlags() { return caseFlags; }
    public void setCaseFlags(List<Map<String, Object>> caseFlags) { this.caseFlags = caseFlags; }
}
