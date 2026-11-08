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
 * Strategy implementation for Spreadsheet XML / Excel test suite export.
 */
public class ExcelExporter extends AbstractExporter {

    @Override
    public void export(Stage stage, ProblemDto project, List<TestCaseDto> cases) {
        String title = (project != null && project.getTitle() != null) ? project.getTitle() : "Problem";
        File file = chooseSaveFile(stage, "Excel Files (*.xls)", "*.xls", title + "_TestSuite.xls");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\"?>\n");
            xml.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xml.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
            xml.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
            xml.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
            xml.append(" <Styles>\n");
            xml.append("  <Style ss:ID=\"Header\">\n");
            xml.append("   <Font ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n");
            xml.append("   <Interior ss:Color=\"#1E293B\" ss:Pattern=\"Solid\"/>\n");
            xml.append("  </Style>\n");
            xml.append("  <Style ss:ID=\"Title\">\n");
            xml.append("   <Font ss:Bold=\"1\" ss:Size=\"14\" ss:Color=\"#0F172A\"/>\n");
            xml.append("  </Style>\n");
            xml.append(" </Styles>\n");
            xml.append(" <Worksheet ss:Name=\"TestCases\">\n");
            xml.append("  <Table>\n");
            xml.append("   <Row><Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">TestForge AI - ").append(escapeXml(title)).append("</Data></Cell></Row>\n");
            xml.append("   <Row><Cell><Data ss:Type=\"String\">Quality Score: ").append(project != null ? project.getQualityScore() : 0).append("/100 | Total Cases: ").append(cases.size()).append("</Data></Cell></Row>\n");
            xml.append("   <Row ss:Index=\"4\" ss:StyleID=\"Header\">\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Test ID</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Category</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Priority</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Requirement Tag</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Input Data</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Expected Output</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Preconditions</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Explanation</Data></Cell>\n");
            xml.append("    <Cell><Data ss:Type=\"String\">Reviewed (Done)</Data></Cell>\n");
            xml.append("   </Row>\n");

            for (TestCaseDto tc : cases) {
                xml.append("   <Row>\n");
                xml.append("    <Cell><Data ss:Type=\"Number\">").append(tc.getId()).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getCategory())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getPriority())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getRequirementTag())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getInput())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getExpectedOutput())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getPreconditions())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(tc.getExplanation())).append("</Data></Cell>\n");
                xml.append("    <Cell><Data ss:Type=\"String\">").append(tc.getIsReviewed() ? "YES" : "NO").append("</Data></Cell>\n");
                xml.append("   </Row>\n");
            }

            xml.append("  </Table>\n");
            xml.append(" </Worksheet>\n");
            xml.append("</Workbook>\n");

            writer.print(xml);
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Export Success", null, "Exported to " + file.getAbsolutePath());
        } catch (Exception e) {
            UIUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Export Failed", null, e.getMessage());
        }
    }

    @Override
    public String getFormatName() {
        return "Excel";
    }

    @Override
    public String getFileExtension() {
        return "*.xls";
    }
}
