package com.testforge.ui.util.export;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Base abstract exporter containing common utility methods for file choosing and escaping.
 */
public abstract class AbstractExporter implements Exporter {

    protected File chooseSaveFile(Stage stage, String description, String ext, String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Test Cases");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, ext));
        chooser.setInitialFileName(defaultName.replaceAll("[^a-zA-Z0-9_.-]", "_"));
        return chooser.showSaveDialog(stage);
    }

    protected String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    protected String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
