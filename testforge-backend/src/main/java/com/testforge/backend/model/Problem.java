package com.testforge.backend.model;

import java.util.Date;
import java.util.Objects;

/**
 * Domain model representing a programming problem / testing project.
 * Encapsulates core metadata, requirements, and test suite health metrics.
 */
public class Problem implements Comparable<Problem> {

    private Long id;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private Date createdAt;
    private Integer testCount = 0;
    private String settings;
    private Integer qualityScore = 0;
    private String coverageSummary;

    public Problem() {
        this.createdAt = new Date();
    }

    public Problem(Long id, String title, String description,
                   String inputFormat, String outputFormat,
                   String constraints) {
        this.id = id;
        setTitle(title);
        this.description = description;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.constraints = constraints;
        this.createdAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = (title != null && !title.isBlank()) ? title.trim() : "Untitled Problem";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public Date getCreatedAt() {
        return createdAt != null ? (Date) createdAt.clone() : null;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt != null ? (Date) createdAt.clone() : new Date();
    }

    public Integer getTestCount() {
        return testCount != null ? testCount : 0;
    }

    public void setTestCount(Integer testCount) {
        this.testCount = (testCount != null && testCount >= 0) ? testCount : 0;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public Integer getQualityScore() {
        return qualityScore != null ? qualityScore : 0;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = (qualityScore != null) ? Math.max(0, Math.min(100, qualityScore)) : 0;
    }

    public String getCoverageSummary() {
        return coverageSummary;
    }

    public void setCoverageSummary(String coverageSummary) {
        this.coverageSummary = coverageSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Problem problem)) return false;
        return Objects.equals(id, problem.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(title, createdAt);
    }

    @Override
    public int compareTo(Problem other) {
        if (other == null) return -1;
        // Natural ordering for Problem: higher qualityScore first
        int scoreCompare = Integer.compare(other.getQualityScore(), this.getQualityScore());
        if (scoreCompare != 0) return scoreCompare;
        if (this.id != null && other.id != null) {
            return Long.compare(other.id, this.id);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Problem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", qualityScore=" + qualityScore +
                ", testCount=" + testCount +
                '}';
    }
}
