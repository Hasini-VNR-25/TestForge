package com.testforge.ui.view.dialog;

import com.testforge.ui.model.TestCaseDto;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class EditTestCaseDialog {

    public static Optional<TestCaseDto> show(TestCaseDto original) {
        Dialog<TestCaseDto> dialog = new Dialog<>();
        dialog.setTitle("Edit Test Case #" + original.getId());
        dialog.setHeaderText("Modify structured attributes for Test Case #" + original.getId());

        ButtonType saveButtonType = new ButtonType("Save to Database", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("NORMAL", "BOUNDARY", "EDGE", "INVALID_INPUT", "EXCEPTION");
        catBox.setValue(original.getCategory().toUpperCase());

        ComboBox<String> prioBox = new ComboBox<>();
        prioBox.getItems().addAll("HIGH", "MEDIUM", "LOW");
        prioBox.setValue(original.getPriority().toUpperCase());

        TextField reqField = new TextField(original.getRequirementTag());
        TextField preField = new TextField(original.getPreconditions());

        TextArea inputArea = new TextArea(original.getInput());
        inputArea.setPrefRowCount(3);

        TextArea outputArea = new TextArea(original.getExpectedOutput());
        outputArea.setPrefRowCount(3);

        TextArea explArea = new TextArea(original.getExplanation());
        explArea.setPrefRowCount(3);

        grid.add(new Label("Category:"), 0, 0);
        grid.add(catBox, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(prioBox, 1, 1);
        grid.add(new Label("Requirement Tag:"), 0, 2);
        grid.add(reqField, 1, 2);
        grid.add(new Label("Preconditions:"), 0, 3);
        grid.add(preField, 1, 3);
        grid.add(new Label("Input Data:"), 0, 4);
        grid.add(inputArea, 1, 4);
        grid.add(new Label("Expected Output:"), 0, 5);
        grid.add(outputArea, 1, 5);
        grid.add(new Label("Explanation:"), 0, 6);
        grid.add(explArea, 1, 6);

        // Apply dark styling
        dialog.getDialogPane().getStylesheets().add(
                EditTestCaseDialog.class.getResource("/styles/style.css").toExternalForm()
        );
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                original.setCategory(catBox.getValue());
                original.setPriority(prioBox.getValue());
                original.setRequirementTag(reqField.getText().trim());
                original.setPreconditions(preField.getText().trim());
                original.setInput(inputArea.getText().trim());
                original.setExpectedOutput(outputArea.getText().trim());
                original.setExplanation(explArea.getText().trim());
                return original;
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
