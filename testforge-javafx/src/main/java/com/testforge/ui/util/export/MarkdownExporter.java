package com.testforge.ui.util.export;

import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.model.TestCaseDto;
import com.testforge.ui.util.UIUtils;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Strategy implementation for GitHub-flavored Markdown test suite export.
 */
public class MarkdownExporter extends AbstractExporter {

    @Override
    public void export(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        String title = (project != null && project.getTitle() != null) ? project.getTitle() : "Problem";
        File file = chooseSaveFile(stage, "Markdown Files (*.md)", "*.md", title + "_TestPlan.md");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("# TestForge AI - Test Specification Report");
            writer.println();
            writer.printf("**Problem Title:** %s  %n", title);
            writer.printf("**Quality Score:** %d/100  %n", project != null ? project.getQualityScore() : 0);
            writer.printf("**Total Test Cases:** %d  %n", cases.size());
            writer.println();
            writer.println("---");
            writer.println();
            writer.println("| ID | Category | Priority | Requirement Tag | Input Data | Expected Output | Explanation |");
            writer.println("|---|---|---|---|---|---|---|");

            for (TestCaseDto tc : cases) {
                writer.printf("| #%d | `%s` | %s | %s | `%s` | `%s` | %s |%n",
                        tc.getId(),
                        tc.getCategory(),
                        tc.getPriority(),
                        tc.getRequirementTag() != null ? tc.getRequirementTag() : "-",
                        tc.getInput() != null ? tc.getInput().replace("|", "\\|") : "",
                        tc.getExpectedOutput() != null ? tc.getExpectedOutput().replace("|", "\\|") : "",
                        tc.getExplanation() != null ? tc.getExplanation().replace("|", "\\|") : ""
                );
            }
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Export Success", null, "Exported to " + file.getAbsolutePath());
        } catch (Exception e) {
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Export Failed", null, e.getMessage());
        }
    }

    @Override
    public String getFormatName() {
        return "Markdown";
    }

    @Override
    public String getFileExtension() {
        return "*.md";
    }
}
