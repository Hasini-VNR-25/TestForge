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
 * Strategy implementation for RFC-4180 CSV test suite export.
 */
public class CsvExporter extends AbstractExporter {

    @Override
    public void export(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        String title = (project != null && project.getTitle() != null) ? project.getTitle() : "Problem";
        File file = chooseSaveFile(stage, "CSV Files (*.csv)", "*.csv", title + "_TestCases.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("ID,Category,Priority,Requirement Tag,Preconditions,Input,Expected Output,Explanation,Reviewed");
            for (TestCaseDto tc : cases) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s%n",
                        tc.getId(),
                        escapeCsv(tc.getCategory()),
                        escapeCsv(tc.getPriority()),
                        escapeCsv(tc.getRequirementTag()),
                        escapeCsv(tc.getPreconditions()),
                        escapeCsv(tc.getInput()),
                        escapeCsv(tc.getExpectedOutput()),
                        escapeCsv(tc.getExplanation()),
                        tc.getIsReviewed() ? "YES" : "NO"
                );
            }
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Export Success", null, "Exported to " + file.getAbsolutePath());
        } catch (Exception e) {
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Export Failed", null, e.getMessage());
        }
    }

    @Override
    public String getFormatName() {
        return "CSV";
    }

    @Override
    public String getFileExtension() {
        return "*.csv";
    }
}
