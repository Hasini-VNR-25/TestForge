package com.testforge.ui.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCaseDto {
    private Long id;
    private Long problemId;
    private String input;
    private String expectedOutput;
    private String category = "NORMAL";
    private String priority = "MEDIUM";
    private String requirementTag = "";
    private String preconditions = "";
    private String explanation = "";
    private final BooleanProperty isReviewed = new SimpleBooleanProperty(false);
    private List<String> auditFlags = new ArrayList<>();
    private String testResult = "PASS";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public String getInput() { return input != null ? input : ""; }
    public void setInput(String input) { this.input = input; }

    public String getExpectedOutput() { return expectedOutput != null ? expectedOutput : ""; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getCategory() { return category != null ? category : "NORMAL"; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority != null ? priority : "MEDIUM"; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getRequirementTag() { return requirementTag != null ? requirementTag : ""; }
    public void setRequirementTag(String requirementTag) { this.requirementTag = requirementTag; }

    public String getPreconditions() { return preconditions != null ? preconditions : ""; }
    public void setPreconditions(String preconditions) { this.preconditions = preconditions; }

    public String getExplanation() { return explanation != null ? explanation : ""; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    @JsonProperty("isReviewed")
    public boolean getIsReviewed() { return isReviewed.get(); }

    @JsonProperty("isReviewed")
    public void setIsReviewed(boolean value) { this.isReviewed.set(value); }

    @JsonIgnore
    public BooleanProperty isReviewedProperty() { return isReviewed; }

    public List<String> getAuditFlags() { return auditFlags; }
    public void setAuditFlags(List<String> auditFlags) { this.auditFlags = auditFlags != null ? auditFlags : new ArrayList<>(); }

    public String getTestResult() {
        if (testResult == null || testResult.isBlank()) {
            if ("Needs manual review".equalsIgnoreCase(expectedOutput)) return "FAIL";
            return "PASS";
        }
        return testResult;
    }
    public void setTestResult(String testResult) { this.testResult = testResult; }
}
