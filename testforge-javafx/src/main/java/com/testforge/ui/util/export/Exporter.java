package com.testforge.ui.util.export;

import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.model.TestCaseDto;
import javafx.stage.Stage;

import java.util.List;

/**
 * Strategy interface defining test suite export behavior.
 * Demonstrates the Gang of Four (GoF) Strategy Pattern.
 */
public interface Exporter {

    /**
     * Executes the export strategy for the provided test cases.
     *
     * @param stage   The JavaFX window stage for the file dialog
     * @param project The active problem / project domain model
     * @param cases   The collection of test cases to export
     */
    void export(Stage stage, ProblemDto project, List<TestCaseDto> cases);

    /**
     * Returns the human-readable format name (e.g. "CSV", "JSON", "Excel").
     */
    String getFormatName();

    /**
     * Returns the primary file extension (e.g. "*.csv", "*.json").
     */
    String getFileExtension();
}
