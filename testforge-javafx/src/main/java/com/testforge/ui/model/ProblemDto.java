package com.testforge.ui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemDto {
    private Long id;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String problemConstraints;
    private String createdAt;
    private Integer testCount;
    private String settings;
    private Integer qualityScore;
    private Object coverageSummary;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInputFormat() { return inputFormat; }
    public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public String getProblemConstraints() { return problemConstraints; }
    public void setProblemConstraints(String problemConstraints) { this.problemConstraints = problemConstraints; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Integer getTestCount() { return testCount != null ? testCount : 0; }
    public void setTestCount(Integer testCount) { this.testCount = testCount; }

    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }

    public Integer getQualityScore() { return qualityScore != null ? qualityScore : 0; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

    public Object getCoverageSummary() { return coverageSummary; }
    public void setCoverageSummary(Object coverageSummary) { this.coverageSummary = coverageSummary; }
}
