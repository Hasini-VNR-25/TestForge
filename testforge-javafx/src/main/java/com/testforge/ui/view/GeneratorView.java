package com.testforge.ui.view;

import com.testforge.ui.client.ApiClient;
import com.testforge.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

public class GeneratorView extends VBox {

    private final ApiClient apiClient;
    private final Consumer<Long> onProjectGenerated;

    private final TextField titleField = new TextField();
    private final TextArea descArea = new TextArea();
    private final TextField inputFormatField = new TextField();
    private final TextField outputFormatField = new TextField();
    private final TextField constraintsField = new TextField();

    private int selectedCount = 10;
    private final List<Button> countButtons = new ArrayList<>();

    private final CheckBox catNormal = new CheckBox("Normal / Positive");
    private final CheckBox catBoundary = new CheckBox("Boundary Values");
    private final CheckBox catEdge = new CheckBox("Edge Cases");
    private final CheckBox catInvalid = new CheckBox("Invalid Inputs");
    private final CheckBox catException = new CheckBox("Exceptions & Errors");

    private final Button generateBtn = new Button("⚡ Generate Test Suite");
    private final Label statusLabel = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label engineUsedLabel = new Label();
    private final VBox fallbackBanner = new VBox(6);
    private final Button viewInManagerBtn = new Button("📋 View Generated Test Suite in Manager ➜");

    public GeneratorView(ApiClient apiClient, Consumer<Long> onProjectGenerated) {
        this.apiClient = apiClient;
        this.onProjectGenerated = onProjectGenerated;

        setSpacing(18);
        setPadding(new Insets(24));

        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("card");

        Label headerLbl = new Label("⚡ Configure & Generate Test Cases");
        headerLbl.getStyleClass().add("card-header-label");

        Label hintLbl = new Label("Provide the problem specification. TestForge AI will analyze the requirements, decompose rules, generate candidate test cases across chosen categories, and persist everything directly into your Oracle database.");
        hintLbl.getStyleClass().add("hint-label");
        hintLbl.setWrapText(true);

        titleField.setPromptText("e.g. Longest Palindromic Substring");
        descArea.setPromptText("Describe the problem, input rules, edge conditions, and requirements...");
        descArea.setPrefRowCount(5);
        inputFormatField.setPromptText("e.g. Single string S of length 1 <= N <= 1000");
        outputFormatField.setPromptText("e.g. Substring of S that is a palindrome");
        constraintsField.setPromptText("e.g. 1 <= s.length <= 1000, English letters only");

        // Configuration Row (Count Pills + Category Chips)
        GridPane configGrid = new GridPane();
        configGrid.setHgap(20);
        configGrid.setVgap(10);
        configGrid.getStyleClass().add("card");
        configGrid.setStyle("-fx-background-color: #0b1122; -fx-padding: 16px;");

        // Count picker
        VBox countBox = new VBox(8);
        Label countLbl = new Label("Select Test Cases Count:");
        countLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        HBox countPills = new HBox(8);
        for (int c : new int[]{5, 10, 15, 20}) {
            Button pill = new Button(c + " Cases");
            pill.getStyleClass().add("pill-btn");
            if (c == 10) pill.getStyleClass().add("pill-btn-active");
            final int countVal = c;
            pill.setOnAction(e -> {
                selectedCount = countVal;
                countButtons.forEach(b -> b.getStyleClass().remove("pill-btn-active"));
                pill.getStyleClass().add("pill-btn-active");
            });
            countButtons.add(pill);
            countPills.getChildren().add(pill);
        }
        countBox.getChildren().addAll(countLbl, countPills);

        // Category picker
        VBox catBox = new VBox(8);
        Label catLbl = new Label("Target Test Categories:");
        catLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        catNormal.setSelected(true);
        catBoundary.setSelected(true);
        catEdge.setSelected(true);
        catInvalid.setSelected(true);
        catException.setSelected(true);

        FlowPane catChips = new FlowPane(12, 8);
        catChips.getChildren().addAll(catNormal, catBoundary, catEdge, catInvalid, catException);
        catBox.getChildren().addAll(catLbl, catChips);

        configGrid.add(countBox, 0, 0);
        configGrid.add(catBox, 1, 0);

        // Button and Status Row
        HBox actionBox = new HBox(14);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        generateBtn.getStyleClass().add("btn-primary");
        generateBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 24px;");
        generateBtn.setOnAction(e -> doGenerate());

        progress.setVisible(false);
        progress.setManaged(false);
        progress.setPrefSize(24, 24);

        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #818cf8; -fx-font-weight: 600;");

        actionBox.getChildren().addAll(generateBtn, progress, statusLabel);

        // Visible Fallback Warning Banner (B7)
        fallbackBanner.setVisible(false);
        fallbackBanner.setManaged(false);
        fallbackBanner.setStyle("-fx-background-color: #451a03; -fx-border-color: #f59e0b; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-padding: 12px; -fx-spacing: 4px;");
        Label fbTitle = new Label("⚠️ Fallback Mode Activated");
        fbTitle.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: 800; -fx-font-size: 13px;");
        Label fbMsg = new Label("Gemini AI was unreachable or encountered an error. Test suite was generated using the Offline Rule-Based Heuristic Engine.");
        fbMsg.setStyle("-fx-text-fill: #fef3c7; -fx-font-size: 12px;");
        fbMsg.setWrapText(true);
        fallbackBanner.getChildren().addAll(fbTitle, fbMsg);

        // Clear Engine Used Badge (B7)
        engineUsedLabel.setVisible(false);
        engineUsedLabel.setManaged(false);
        engineUsedLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #38bdf8; -fx-background-color: #0f172a; -fx-padding: 6px 14px; -fx-border-color: #0284c7; -fx-border-radius: 4px;");

        // View in Manager button
        viewInManagerBtn.setVisible(false);
        viewInManagerBtn.setManaged(false);
        viewInManagerBtn.getStyleClass().add("btn-success");
        viewInManagerBtn.setStyle("-fx-font-size: 13px; -fx-padding: 8px 20px;");

        formCard.getChildren().addAll(
                headerLbl, hintLbl,
                new Label("Problem Title *"), titleField,
                new Label("Problem Statement & Requirements *"), descArea,
                new Label("Input Format (Optional)"), inputFormatField,
                new Label("Output Format (Optional)"), outputFormatField,
                new Label("Constraints & Edge Requirements (Optional)"), constraintsField,
                configGrid,
                actionBox,
                engineUsedLabel,
                fallbackBanner,
                viewInManagerBtn
        );

        getChildren().add(formCard);
    }

    private void doGenerate() {
        String title = titleField.getText().trim();
        String desc = descArea.getText().trim();

        if (title.isEmpty() || desc.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", null, "Please enter both a Problem Title and Problem Statement.");
            return;
        }

        // Quick length check for instant feedback (A5)
        if (desc.length() < 15) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", null, "Problem statement is too short (" + desc.length() + " chars). Minimum required is 15 characters.");
            return;
        }

        List<String> categories = new ArrayList<>();
        if (catNormal.isSelected()) categories.add("NORMAL");
        if (catBoundary.isSelected()) categories.add("BOUNDARY");
        if (catEdge.isSelected()) categories.add("EDGE");
        if (catInvalid.isSelected()) categories.add("INVALID_INPUT");
        if (catException.isSelected()) categories.add("EXCEPTION");

        if (categories.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", null, "Please select at least one test category.");
            return;
        }

        StringBuilder fullStatement = new StringBuilder();
        fullStatement.append("Problem title: ").append(title).append("\n\n");
        fullStatement.append("Problem description:\n").append(desc).append("\n\n");
        if (!inputFormatField.getText().isBlank()) fullStatement.append("Input format:\n").append(inputFormatField.getText().trim()).append("\n\n");
        if (!outputFormatField.getText().isBlank()) fullStatement.append("Output format:\n").append(outputFormatField.getText().trim()).append("\n\n");
        if (!constraintsField.getText().isBlank()) fullStatement.append("Constraints:\n").append(constraintsField.getText().trim());

        Map<String, Object> req = new HashMap<>();
        req.put("title", title);
        req.put("description", desc);
        req.put("problemStatement", fullStatement.toString());
        req.put("testCount", selectedCount);
        req.put("categories", categories);

        generateBtn.setDisable(true);
        progress.setVisible(true);
        progress.setManaged(true);
        statusLabel.setText("Connecting to AI and analyzing test coverage...");
        engineUsedLabel.setVisible(false);
        engineUsedLabel.setManaged(false);
        fallbackBanner.setVisible(false);
        fallbackBanner.setManaged(false);
        viewInManagerBtn.setVisible(false);
        viewInManagerBtn.setManaged(false);

        apiClient.generateTests(req)
                .thenAccept(res -> Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    progress.setVisible(false);
                    progress.setManaged(false);

                    long newId = res.path("projectId").asLong(0);
                    int testCount = res.path("testCount").asInt(selectedCount);
                    String engineUsed = res.path("engineUsed").asText("Offline Rule-Based Heuristic Generator");
                    boolean isFallback = res.path("isFallback").asBoolean(false);

                    statusLabel.setText("Generated " + testCount + " test cases successfully!");

                    // Show engineUsed clearly after generation (B7)
                    engineUsedLabel.setText("⚙️ Engine Used: " + engineUsed);
                    engineUsedLabel.setVisible(true);
                    engineUsedLabel.setManaged(true);

                    // Show fallback banner if result came from fallback (B7)
                    if (isFallback) {
                        fallbackBanner.setVisible(true);
                        fallbackBanner.setManaged(true);
                    } else {
                        fallbackBanner.setVisible(false);
                        fallbackBanner.setManaged(false);
                    }

                    // Open in Manager navigation action
                    viewInManagerBtn.setText("📋 Open Test Suite in Manager (" + testCount + " Cases) ➜");
                    viewInManagerBtn.setVisible(true);
                    viewInManagerBtn.setManaged(true);
                    viewInManagerBtn.setOnAction(ev -> onProjectGenerated.accept(newId));

                    // Auto-navigate to manager view so user immediately sees the generated test suite and specification details
                    onProjectGenerated.accept(newId);
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> {
                        generateBtn.setDisable(false);
                        progress.setVisible(false);
                        progress.setManaged(false);
                        statusLabel.setText("Generation failed.");
                        engineUsedLabel.setVisible(false);
                        engineUsedLabel.setManaged(false);
                        fallbackBanner.setVisible(false);
                        fallbackBanner.setManaged(false);
                        viewInManagerBtn.setVisible(false);
                        viewInManagerBtn.setManaged(false);
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Generation Failed", null, err.getMessage());
                    });
                    return null;
                });
    }
}
