package com.testforge.backend.model;

import com.testforge.backend.exception.InvalidTestCaseException;
import java.util.Objects;

/**
 * Domain model representing a single test case.
 * Fully encapsulated with invariant validation, enum integration,
 * and natural ordering by Priority for PriorityQueue collections.
 */
public class TestCase implements Comparable<TestCase> {

    private Long id;
    private Long problemId;
    private String input;
    private String expectedOutput;
    private Category category = Category.NORMAL;
    private String explanation;
    private Priority priority = Priority.MEDIUM;
    private String preconditions = "Standard input";
    private Boolean isReviewed = false;
    private String requirementTag;

    public TestCase() {
    }

    public TestCase(Long id, Long problemId, String input,
                    String expectedOutput, String category) {
        this.id = id;
        this.problemId = problemId;
        setInput(input);
        setExpectedOutput(expectedOutput);
        setCategory(category);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidTestCaseException("Test case input data cannot be null or empty.");
        }
        this.input = input.trim();
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        if (expectedOutput == null) {
            throw new InvalidTestCaseException("Expected output cannot be null.");
        }
        this.expectedOutput = expectedOutput.trim();
    }

    public String getCategory() {
        return category != null ? category.name() : Category.NORMAL.name();
    }

    public Category getCategoryEnum() {
        return category != null ? category : Category.NORMAL;
    }

    public void setCategory(String category) {
        this.category = Category.from(category);
    }

    public void setCategory(Category category) {
        this.category = (category != null) ? category : Category.NORMAL;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation != null ? explanation.trim() : null;
    }

    public String getPriority() {
        return priority != null ? priority.name() : Priority.MEDIUM.name();
    }

    public Priority getPriorityEnum() {
        return priority != null ? priority : Priority.MEDIUM;
    }

    public void setPriority(String priority) {
        this.priority = Priority.from(priority);
    }

    public void setPriority(Priority priority) {
        this.priority = (priority != null) ? priority : Priority.MEDIUM;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions != null ? preconditions.trim() : "Standard input";
    }

    public Boolean getIsReviewed() {
        return isReviewed;
    }

    public void setIsReviewed(Boolean isReviewed) {
        this.isReviewed = (isReviewed != null) && isReviewed;
    }

    public String getRequirementTag() {
        return requirementTag;
    }

    public void setRequirementTag(String requirementTag) {
        this.requirementTag = requirementTag != null ? requirementTag.trim() : null;
    }

    @Override
    public int compareTo(TestCase other) {
        if (other == null) return 1;
        // Priority level (1=HIGH, 2=MEDIUM, 3=LOW)
        int pDiff = Integer.compare(this.getPriorityEnum().getLevel(), other.getPriorityEnum().getLevel());
        if (pDiff != 0) return pDiff;

        // If priorities match, sort by ID ascending
        if (this.id != null && other.id != null) {
            return this.id.compareTo(other.id);
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestCase tc)) return false;
        if (id != null && tc.id != null) {
            return Objects.equals(id, tc.id);
        }
        return Objects.equals(input, tc.input) && Objects.equals(expectedOutput, tc.expectedOutput);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(input, expectedOutput);
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "id=" + id +
                ", category=" + getCategory() +
                ", priority=" + getPriority() +
                ", input='" + input + '\'' +
                ", expectedOutput='" + expectedOutput + '\'' +
                '}';
    }
}
