package com.testforge.ui.util.export;

import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.model.TestCaseDto;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

/**
 * Strategy Context class that holds a reference to a concrete Exporter strategy
 * and delegates the export execution at runtime.
 */
public class ExportContext {

    private Exporter strategy;

    public ExportContext(Exporter strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy must not be null");
    }

    public void setStrategy(Exporter strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy must not be null");
    }

    public Exporter getStrategy() {
        return strategy;
    }

    public void executeExport(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        strategy.export(stage, project, cases);
    }
}
