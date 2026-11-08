package com.testforge.ui.util;

import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.model.TestCaseDto;
import com.testforge.ui.util.export.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Facade coordinating test suite export operations.
 * Delegates concrete formatting to Strategy Pattern Exporters (CsvExporter, ExcelExporter, MarkdownExporter, JsonExporter).
 */
public class ExportUtils {

    public static void exportCsv(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        new ExportContext(new CsvExporter()).executeExport(stage, project, cases);
    }

    public static void exportExcel(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        new ExportContext(new ExcelExporter()).executeExport(stage, project, cases);
    }

    public static void exportMarkdown(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        new ExportContext(new MarkdownExporter()).executeExport(stage, project, cases);
    }

    public static void exportJson(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        new ExportContext(new JsonExporter()).executeExport(stage, project, cases);
    }

    public static void openExecutiveReport(ProblemDto project, List<TestCaseDto> cases) {
        exportReport(null, project, cases);
    }

    public static void exportReport(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        try {
            File tempFile = File.createTempFile("TestForge_Executive_Report_", ".html");
            tempFile.deleteOnExit();

            StringBuilder rows = new StringBuilder();
            for (TestCaseDto tc : cases) {
                String cat = tc.getCategory() != null ? tc.getCategory() : "NORMAL";
                String pri = tc.getPriority() != null ? tc.getPriority() : "MEDIUM";
                String catBadge = "<span style=\"padding: 2px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; background: #e0f2fe; color: #0284c7;\">" + escapeXml(cat) + "</span>";
                String priBadge = "<span style=\"padding: 2px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; background: #fef3c7; color: #d97706;\">" + escapeXml(pri) + "</span>";

                rows.append("""
                        <tr>
                            <td style="font-weight: 600; font-family: monospace;">#%d</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td style="font-family: monospace; font-size: 11px;">%s</td>
                            <td><code>%s</code></td>
                            <td><code>%s</code></td>
                            <td style="color: #64748b; font-size: 12px;">%s</td>
                            <td style="text-align: center;">%s</td>
                        </tr>
                        """.formatted(
                        tc.getId(),
                        catBadge,
                        priBadge,
                        escapeXml(tc.getRequirementTag() != null ? tc.getRequirementTag() : "-"),
                        escapeXml(tc.getInput()),
                        escapeXml(tc.getExpectedOutput()),
                        escapeXml(tc.getExplanation()),
                        tc.getIsReviewed() ? "✅" : "⚪"
                ));
            }

            String html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>TestForge AI - Executive Test Suite Report</title>
                        <style>
                            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #f8fafc; color: #0f172a; margin: 0; padding: 40px; }
                            .header { background: #0f172a; color: #ffffff; padding: 32px; border-radius: 12px; margin-bottom: 30px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
                            .header h1 { margin: 0 0 10px 0; font-size: 24px; font-weight: 700; }
                            .header p { margin: 0; color: #94a3b8; font-size: 14px; }
                            .meta-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: 24px; }
                            .meta-card { background: rgba(255, 255, 255, 0.05); padding: 12px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1); }
                            .meta-card .val { font-size: 18px; font-weight: 700; color: #38bdf8; }
                            .meta-card .lbl { font-size: 11px; text-transform: uppercase; color: #94a3b8; margin-top: 4px; }
                            table { width: 100%; border-collapse: separate; border-spacing: 0; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; }
                            th { background: #f1f5f9; padding: 12px 16px; text-align: left; font-size: 12px; text-transform: uppercase; color: #475569; border-bottom: 1px solid #e2e8f0; }
                            td { padding: 14px 16px; font-size: 13px; border-bottom: 1px solid #f1f5f9; }
                            tr:last-child td { border-bottom: none; }
                            code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 12px; color: #0f172a; border: 1px solid #e2e8f0; }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <h1>⚡ TestForge AI · Executive Test Verification Report</h1>
                            <p>Formal Requirements Traceability & Software Quality Certification</p>
                            <div class="meta-grid">
                                <div class="meta-card"><div class="val">%s</div><div class="lbl">Project Title</div></div>
                                <div class="meta-card"><div class="val">#%d</div><div class="lbl">Problem ID</div></div>
                                <div class="meta-card"><div class="val">%d/100</div><div class="lbl">Quality Health Score</div></div>
                                <div class="meta-card"><div class="val">%d Cases</div><div class="lbl">Test Suite Size</div></div>
                            </div>
                        </div>

                        <table>
                            <thead>
                                <tr>
                                    <th style="width: 50px;">ID</th>
                                    <th style="width: 100px;">Category</th>
                                    <th style="width: 80px;">Priority</th>
                                    <th style="width: 140px;">Requirement Tag</th>
                                    <th>Input Data</th>
                                    <th>Expected Output</th>
                                    <th>Rationale / Explanation</th>
                                    <th style="width: 60px; text-align: center;">Reviewed</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </body>
                    </html>
                    """.formatted(
                    escapeXml(project.getTitle()),
                    project.getId() != null ? project.getId() : 0,
                    project.getQualityScore(),
                    cases.size(),
                    rows.toString()
            );

            Files.writeString(tempFile.toPath(), html, StandardCharsets.UTF_8);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tempFile.toURI());
            } else {
                UIUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Report Generated", null, "Report written to " + tempFile.getAbsolutePath());
            }
        } catch (Exception e) {
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Report Error", null, e.getMessage());
        }
    }

    private static String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
