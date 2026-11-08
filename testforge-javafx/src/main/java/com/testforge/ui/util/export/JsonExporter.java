package com.testforge.ui.util.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.model.TestCaseDto;
import com.testforge.ui.util.UIUtils;
import javafx.stage.Stage;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for structured JSON test suite export.
 */
public class JsonExporter extends AbstractExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void export(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        String title = (project != null && project.getTitle() != null) ? project.getTitle() : "Problem";
        File file = chooseSaveFile(stage, "JSON Files (*.json)", "*.json", title + "_TestCases.json");
        if (file == null) return;

        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("projectTitle", title);
            root.put("projectId", project != null ? project.getId() : null);
            root.put("qualityScore", project != null ? project.getQualityScore() : 0);
            root.put("totalTestCases", cases.size());
            root.put("testCases", cases);

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Export Success", null, "Exported to " + file.getAbsolutePath());
        } catch (Exception e) {
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Export Failed", null, e.getMessage());
        }
    }

    @Override
    public String getFormatName() {
        return "JSON";
    }

    @Override
    public String getFileExtension() {
        return "*.json";
    }
}
