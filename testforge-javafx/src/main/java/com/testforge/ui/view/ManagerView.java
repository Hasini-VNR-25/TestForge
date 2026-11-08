package com.testforge.ui.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.ui.client.ApiClient;
import com.testforge.ui.model.*;
import com.testforge.ui.util.ExportUtils;
import com.testforge.ui.util.UIUtils;
import com.testforge.ui.view.dialog.EditTestCaseDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ManagerView extends VBox {

    private final ApiClient apiClient;
    private final Stage primaryStage;

    private ProblemDto currentProject = null;
    private final ObservableList<TestCaseDto> masterCases = FXCollections.observableArrayList();
    private final FilteredList<TestCaseDto> filteredCases = new FilteredList<>(masterCases, t -> true);

    // Headers
    private final Label titleLabel = new Label("📋 Test Case Manager");
    private final Label metaLabel = new Label("Select a project from history or generate a new test suite.");
    private final Label qualityScoreBadge = new Label("Quality Score: -/100");
    private final Label settingsBadge = new Label();

    // Problem Specification Section (displays problem description & details entered during generation)
    private final VBox problemSpecCard = new VBox(8);
    private final Label specTitleLabel = new Label("📝 Problem Statement & Specification Details");
    private final TextArea problemDescArea = new TextArea();
    private final Button btnToggleSpec = new Button("▲ Hide Details");
    private final VBox specContentBox = new VBox(8);

    // Sub-Tabs
    private final Button tabCasesBtn = new Button("📋 Test Cases Table (0)");
    private final Button tabRtmBtn = new Button("📐 Traceability Matrix (RTM)");

    private final VBox casesSubPanel = new VBox(10);
    private final VBox rtmSubPanel = new VBox(10);

    // Sub-Panel 1: Cases Table & Quality Audit
    private final VBox auditBanner = new VBox(8);
    private final Label auditHealthBadge = new Label("Audit: -");
    private final Label auditSummary = new Label();
    private final VBox auditIssuesList = new VBox(4);
    private final Button btnAutoRemoveDuplicates = new Button("🗑️ Auto-Remove Flagged Duplicates");
    private AuditResultDto currentAudit = null;

    private final Label reqsAnalyzedText = new Label();
    private final Label gapsText = new Label();

    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final ComboBox<String> reviewFilter = new ComboBox<>();
    private final TableView<TestCaseDto> casesTable = new TableView<>();
    private final Button btnRegenerate = new Button("\u21BB Regenerate Test Cases");

    // OOP Collections: Deque as an Undo Stack for deleted test cases
    private final Deque<TestCaseDto> undoStack = new ArrayDeque<>();
    private final Button btnUndoDelete = new Button("\u21A9 Undo Delete");
    private final Button btnSortPriority = new Button("\u26A1 Priority Sort");

    // Sub-Panel 2: RTM
    private final Label rtmCoverageScore = new Label("100% Coverage");
    private final TableView<Map<String, String>> rtmTable = new TableView<>();
    private final ObservableList<Map<String, String>> rtmData = FXCollections.observableArrayList();

    public ManagerView(ApiClient apiClient, Stage primaryStage) {
        this.apiClient = apiClient;
        this.primaryStage = primaryStage;

        setSpacing(12);
        setPadding(new Insets(16, 20, 16, 20));

        setupHeader();
        setupSubTabs();
        setupCasesPanel();
        setupRtmPanel();

        getChildren().addAll(
                buildTopProjectCard(),
                casesSubPanel,
                rtmSubPanel
        );

        switchSubPanel("cases");
    }

    private VBox buildTopProjectCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        // Top line: Project title & Export buttons
        HBox topBox = new HBox(12);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(titleLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnReport = new Button("📄 Executive Report (PDF)");
        btnReport.getStyleClass().add("btn-success");
        btnReport.setOnAction(e -> {
            if (currentProject != null) ExportUtils.openExecutiveReport(currentProject, masterCases);
        });

        Button btnExcel = new Button("📊 Export Excel");
        btnExcel.getStyleClass().add("btn-outline");
        btnExcel.setOnAction(e -> {
            if (currentProject != null) ExportUtils.exportExcel(primaryStage, currentProject, masterCases);
        });

        Button btnCsv = new Button("CSV");
        btnCsv.getStyleClass().add("btn-outline");
        btnCsv.setOnAction(e -> {
            if (currentProject != null) ExportUtils.exportCsv(primaryStage, currentProject, masterCases);
        });

        Button btnMd = new Button("Markdown");
        btnMd.getStyleClass().add("btn-outline");
        btnMd.setOnAction(e -> {
            if (currentProject != null) ExportUtils.exportMarkdown(primaryStage, currentProject, masterCases);
        });

        Button btnJson = new Button("JSON");
        btnJson.getStyleClass().add("btn-outline");
        btnJson.setOnAction(e -> {
            if (currentProject != null) ExportUtils.exportJson(primaryStage, currentProject, masterCases);
        });

        btnRegenerate.getStyleClass().add("btn-primary");
        btnRegenerate.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6px 14px; -fx-cursor: hand;");
        btnRegenerate.setOnAction(e -> regenerateTestCases());

        HBox exportsBox = new HBox(8, btnRegenerate, btnReport, btnExcel, btnCsv, btnMd, btnJson);
        exportsBox.setAlignment(Pos.CENTER_RIGHT);

        topBox.getChildren().addAll(titleBox, spacer, exportsBox);

        // Quality score line
        HBox qualityLine = new HBox(12);
        qualityLine.setAlignment(Pos.CENTER_LEFT);
        qualityScoreBadge.getStyleClass().addAll("badge", "badge-normal");
        qualityScoreBadge.setStyle("-fx-font-size: 13px; -fx-padding: 4px 12px; -fx-font-weight: 800;");

        settingsBadge.getStyleClass().addAll("badge", "badge-normal");
        settingsBadge.setStyle("-fx-font-size: 12px; -fx-padding: 4px 10px; -fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-border-color: #334155; -fx-border-radius: 4px;");
        settingsBadge.setVisible(false);
        settingsBadge.setManaged(false);

        qualityLine.getChildren().addAll(qualityScoreBadge, settingsBadge);

        // Problem Specification Section
        problemSpecCard.setStyle("-fx-background-color: #0b1122; -fx-border-color: #1e293b; -fx-border-radius: 8px; -fx-padding: 12px;");

        HBox specHeader = new HBox(10);
        specHeader.setAlignment(Pos.CENTER_LEFT);
        specTitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #38bdf8;");

        Region specSpacer = new Region();
        HBox.setHgrow(specSpacer, Priority.ALWAYS);

        btnToggleSpec.getStyleClass().add("btn-outline");
        btnToggleSpec.setStyle("-fx-font-size: 11px; -fx-padding: 3px 10px;");
        btnToggleSpec.setOnAction(e -> {
            boolean visible = !specContentBox.isVisible();
            specContentBox.setVisible(visible);
            specContentBox.setManaged(visible);
            btnToggleSpec.setText(visible ? "\u25B2 Hide Details" : "\u25BC Show Details");
        });

        specHeader.getChildren().addAll(specTitleLabel, specSpacer, btnToggleSpec);

        problemDescArea.setEditable(false);
        problemDescArea.setWrapText(true);
        problemDescArea.setPrefRowCount(6);
        problemDescArea.setStyle("-fx-control-inner-background: #060913; -fx-text-fill: #e2e8f0; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-border-color: #1e293b; -fx-border-radius: 6px;");

        specContentBox.getChildren().setAll(problemDescArea);
        problemSpecCard.getChildren().setAll(specHeader, specContentBox);
        problemSpecCard.setVisible(false);
        problemSpecCard.setManaged(false);

        // Sub Navigation Tabs Bar
        HBox subTabsBar = new HBox(10);
        subTabsBar.getStyleClass().add("sub-tabs");
        tabCasesBtn.getStyleClass().add("sub-tab-btn");
        tabRtmBtn.getStyleClass().add("sub-tab-btn");

        tabCasesBtn.setOnAction(e -> switchSubPanel("cases"));
        tabRtmBtn.setOnAction(e -> switchSubPanel("rtm"));

        subTabsBar.getChildren().addAll(tabCasesBtn, tabRtmBtn);

        card.getChildren().addAll(topBox, qualityLine, problemSpecCard, subTabsBar);
        return card;
    }

    private void setupHeader() {}
    private void setupSubTabs() {}

    private void switchSubPanel(String panel) {
        casesSubPanel.setVisible("cases".equals(panel));
        casesSubPanel.setManaged("cases".equals(panel));

        rtmSubPanel.setVisible("rtm".equals(panel));
        rtmSubPanel.setManaged("rtm".equals(panel));

        tabCasesBtn.getStyleClass().remove("sub-tab-btn-active");
        tabRtmBtn.getStyleClass().remove("sub-tab-btn-active");

        if ("cases".equals(panel)) tabCasesBtn.getStyleClass().add("sub-tab-btn-active");
        else if ("rtm".equals(panel)) tabRtmBtn.getStyleClass().add("sub-tab-btn-active");
    }

    // ==========================================
    // Sub-Panel 1: Test Cases Table & Audit
    // ==========================================
    private void setupCasesPanel() {
        // Audit Banner
        auditBanner.getStyleClass().add("audit-banner-warn");
        auditBanner.setVisible(false);
        auditBanner.setManaged(false);

        HBox bannerTop = new HBox(10);
        bannerTop.setAlignment(Pos.CENTER_LEFT);
        auditHealthBadge.getStyleClass().addAll("badge", "badge-invalid");
        auditSummary.setStyle("-fx-font-weight: 700; -fx-text-fill: #ffffff; -fx-font-size: 13px;");

        Region bannerSpacer = new Region();
        HBox.setHgrow(bannerSpacer, Priority.ALWAYS);

        btnAutoRemoveDuplicates.getStyleClass().add("btn-danger");
        btnAutoRemoveDuplicates.setOnAction(e -> autoRemoveDuplicates());

        Button btnReAudit = new Button("🔍 Re-Audit Quality");
        btnReAudit.getStyleClass().add("btn-outline");
        btnReAudit.setOnAction(e -> runAudit());

        bannerTop.getChildren().addAll(auditHealthBadge, auditSummary, bannerSpacer, btnAutoRemoveDuplicates, btnReAudit);
        auditBanner.getChildren().addAll(bannerTop, auditIssuesList);

        // Coverage Analysis Box
        GridPane analysisGrid = new GridPane();
        analysisGrid.setHgap(16);
        analysisGrid.setVgap(12);
        analysisGrid.getStyleClass().add("card");
        analysisGrid.setStyle("-fx-background-color: #0b1122; -fx-padding: 16px;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        analysisGrid.getColumnConstraints().addAll(col1, col2);

        VBox reqsBox = new VBox(6);
        Label reqsTitle = new Label("📋 Requirements Analyzed:");
        reqsTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #818cf8; -fx-font-size: 12px;");
        reqsAnalyzedText.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        reqsAnalyzedText.setWrapText(true);
        reqsBox.getChildren().addAll(reqsTitle, reqsAnalyzedText);

        VBox gapsBox = new VBox(6);
        Label gapsTitle = new Label("⚠️ Potential Gaps & Edge Considerations:");
        gapsTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #fbbf24; -fx-font-size: 12px;");
        gapsText.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        gapsText.setWrapText(true);
        gapsBox.getChildren().addAll(gapsTitle, gapsText);

        analysisGrid.add(reqsBox, 0, 0);
        analysisGrid.add(gapsBox, 1, 0);

        // Filters Toolbar
        HBox filterBar = new HBox(10);
        filterBar.getStyleClass().add("card");
        filterBar.setPadding(new Insets(6, 12, 6, 12));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("🔍 Search by input, output, explanation, tag...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        categoryFilter.getItems().addAll("ALL", "NORMAL", "BOUNDARY", "EDGE", "INVALID_INPUT", "EXCEPTION");
        categoryFilter.setValue("ALL");

        priorityFilter.getItems().addAll("ALL", "HIGH", "MEDIUM", "LOW");
        priorityFilter.setValue("ALL");

        reviewFilter.getItems().addAll("ALL", "REVIEWED", "UNREVIEWED");
        reviewFilter.setValue("ALL");

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        priorityFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        reviewFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        btnUndoDelete.getStyleClass().add("btn-outline");
        btnUndoDelete.setDisable(true);
        btnUndoDelete.setOnAction(e -> {
            if (!undoStack.isEmpty() && currentProject != null) {
                TestCaseDto restored = undoStack.pop();
                if (undoStack.isEmpty()) {
                    btnUndoDelete.setDisable(true);
                    btnUndoDelete.setText("\u21A9 Undo Delete");
                } else {
                    btnUndoDelete.setText("↩ Undo (" + undoStack.size() + ")");
                }
                apiClient.createTestCase(currentProject.getId(), restored)
                        .thenAccept(created -> Platform.runLater(() -> {
                            masterCases.add(created != null ? created : restored);
                            updateTabCounts();
                            runAudit();
                            updateTableHeight();
                        }))
                        .exceptionally(err -> {
                            Platform.runLater(() -> {
                                masterCases.add(restored);
                                updateTabCounts();
                                runAudit();
                                updateTableHeight();
                            });
                            return null;
                        });
            }
        });

        btnSortPriority.getStyleClass().add("btn-outline");
        btnSortPriority.setOnAction(e -> {
            // Demonstrates PriorityQueue with custom Comparator to sort test cases by Priority
            PriorityQueue<TestCaseDto> pq = new PriorityQueue<>((a, b) -> {
                int levelA = "HIGH".equalsIgnoreCase(a.getPriority()) ? 1 : ("MEDIUM".equalsIgnoreCase(a.getPriority()) ? 2 : 3);
                int levelB = "HIGH".equalsIgnoreCase(b.getPriority()) ? 1 : ("MEDIUM".equalsIgnoreCase(b.getPriority()) ? 2 : 3);
                return Integer.compare(levelA, levelB);
            });
            pq.addAll(masterCases);
            masterCases.clear();
            while (!pq.isEmpty()) {
                masterCases.add(pq.poll());
            }
            applyFilters();
        });

        filterBar.getChildren().addAll(
                searchField,
                new Label("Category:"), categoryFilter,
                new Label("Priority:"), priorityFilter,
                new Label("Review:"), reviewFilter,
                btnSortPriority,
                btnUndoDelete
        );

        // Test Cases Table
        setupCasesTable();

        casesSubPanel.getChildren().addAll(auditBanner, analysisGrid, filterBar, casesTable);
    }

    private void setupCasesTable() {
        casesTable.setItems(filteredCases);
        casesTable.setPlaceholder(new Label("No test cases found."));
        casesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        casesTable.setFixedCellSize(48.0);
        casesTable.setEditable(true);

        // Done / Reviewed Checkbox Column
        TableColumn<TestCaseDto, Boolean> doneCol = new TableColumn<>("Reviewed");
        doneCol.setPrefWidth(70);
        doneCol.setMaxWidth(80);
        doneCol.setStyle("-fx-alignment: CENTER;");
        doneCol.setCellValueFactory(d -> d.getValue().isReviewedProperty());
        doneCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                setAlignment(Pos.CENTER);
                checkBox.setOnAction(e -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        TestCaseDto tc = getTableView().getItems().get(index);
                        if (tc != null) {
                            boolean newState = checkBox.isSelected();
                            tc.setIsReviewed(newState);
                            apiClient.toggleReview(tc.getId(), newState)
                                    .exceptionally(err -> {
                                        Platform.runLater(() -> {
                                            tc.setIsReviewed(!newState);
                                            checkBox.setSelected(!newState);
                                            UIUtils.showAlert(Alert.AlertType.ERROR, "Review Update Failed",
                                                    "Could not update review status for test case #" + tc.getId(),
                                                    err.getMessage());
                                        });
                                        return null;
                                    });
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                }
            }
        });

        TableColumn<TestCaseDto, String> idCol = new TableColumn<>("# ID");
        idCol.setPrefWidth(48);
        idCol.setMaxWidth(55);
        idCol.setStyle("-fx-alignment: CENTER-LEFT;");
        idCol.setCellValueFactory(d -> {
            Long id = d.getValue() != null ? d.getValue().getId() : null;
            return new SimpleStringProperty(id != null ? "#" + id : "-");
        });

        TableColumn<TestCaseDto, String> catCol = new TableColumn<>("Category");
        catCol.setPrefWidth(105);
        catCol.setMaxWidth(125);
        catCol.setStyle("-fx-alignment: CENTER-LEFT;");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        catCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                    VBox box = new VBox(1);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.getChildren().add(UIUtils.createCategoryBadge(item));
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        TestCaseDto tc = getTableView().getItems().get(getIndex());
                        if (tc != null && !tc.getAuditFlags().isEmpty()) {
                            Label warn = new Label("⚠️ " + tc.getAuditFlags().get(0));
                            warn.setStyle("-fx-font-size: 9px; -fx-text-fill: #fbbf24;");
                            box.getChildren().add(warn);
                        }
                    }
                    setGraphic(box);
                }
            }
        });

        TableColumn<TestCaseDto, String> prioCol = new TableColumn<>("Priority");
        prioCol.setPrefWidth(72);
        prioCol.setMaxWidth(80);
        prioCol.setStyle("-fx-alignment: CENTER-LEFT;");
        prioCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPriority()));
        prioCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setGraphic(UIUtils.createPriorityBadge(item));
                }
            }
        });

        TableColumn<TestCaseDto, String> tagCol = new TableColumn<>("Requirement Tag");
        tagCol.setPrefWidth(125);
        tagCol.setStyle("-fx-alignment: CENTER-LEFT;");
        tagCol.setCellValueFactory(d -> {
            String tag = d.getValue() != null ? d.getValue().getRequirementTag() : "";
            return new SimpleStringProperty(tag != null && !tag.isBlank() ? tag : "-");
        });
        tagCol.setCellFactory(col -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(14));
                text.setStyle("-fx-fill: #cbd5e1; -fx-font-size: 11px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    setText(null);
                    text.setText(item);
                    setGraphic(text);
                    setTooltip(new Tooltip(item));
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<TestCaseDto, String> inputCol = new TableColumn<>("Input Data");
        inputCol.setPrefWidth(140);
        inputCol.setStyle("-fx-alignment: CENTER-LEFT;");
        inputCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInput()));
        inputCol.setCellFactory(col -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(14));
                text.setStyle("-fx-font-family: monospace; -fx-fill: #e2e8f0; -fx-font-size: 11px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    setText(null);
                    text.setText(item);
                    setGraphic(text);
                    setTooltip(new Tooltip(item));
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<TestCaseDto, String> outputCol = new TableColumn<>("Expected Output");
        outputCol.setPrefWidth(140);
        outputCol.setStyle("-fx-alignment: CENTER-LEFT;");
        outputCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExpectedOutput()));
        outputCol.setCellFactory(col -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(14));
                text.setStyle("-fx-font-family: monospace; -fx-fill: #e2e8f0; -fx-font-size: 11px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    setText(null);
                    text.setText(item);
                    setGraphic(text);
                    setTooltip(new Tooltip(item));
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<TestCaseDto, String> resultCol = new TableColumn<>("Result (Pass/Fail)");
        resultCol.setPrefWidth(105);
        resultCol.setMaxWidth(125);
        resultCol.setStyle("-fx-alignment: CENTER;");
        resultCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTestResult()));
        resultCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setStyle("-fx-cursor: hand; -fx-padding: 3px 8px; -fx-font-weight: 700; -fx-font-size: 11px; -fx-background-radius: 4px;");
                btn.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        TestCaseDto tc = getTableView().getItems().get(getIndex());
                        String cur = tc.getTestResult();
                        String next = "PASS".equalsIgnoreCase(cur) ? "FAIL" : "PASS";
                        tc.setTestResult(next);
                        updateBtn(next);
                    }
                });
            }

            private void updateBtn(String res) {
                if ("FAIL".equalsIgnoreCase(res)) {
                    btn.setText("\u2716 FAIL");
                    btn.setStyle("-fx-cursor: hand; -fx-padding: 3px 8px; -fx-font-weight: 700; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-border-color: #ef4444; -fx-border-radius: 4px;");
                } else {
                    btn.setText("\u2714 PASS");
                    btn.setStyle("-fx-cursor: hand; -fx-padding: 3px 8px; -fx-font-weight: 700; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-border-color: #10b981; -fx-border-radius: 4px;");
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    updateBtn(item);
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<TestCaseDto, String> explCol = new TableColumn<>("Preconditions & Explanation");
        explCol.setPrefWidth(380);
        explCol.setStyle("-fx-alignment: CENTER-LEFT;");
        explCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExplanation()));
        explCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);

                    TestCaseDto tc = (getIndex() >= 0 && getIndex() < getTableView().getItems().size())
                            ? getTableView().getItems().get(getIndex()) : null;

                    TextFlow flow = new TextFlow();
                    flow.setMaxWidth(Double.MAX_VALUE);
                    flow.maxWidthProperty().bind(explCol.widthProperty().subtract(16));

                    if (tc != null && !tc.getPreconditions().isBlank()) {
                        Text preText = new Text("[Pre: " + tc.getPreconditions() + "] ");
                        preText.setStyle("-fx-fill: #818cf8; -fx-font-weight: bold; -fx-font-size: 11px;");
                        flow.getChildren().add(preText);
                    }

                    Text explText = new Text(item);
                    explText.setStyle("-fx-fill: #cbd5e1; -fx-font-size: 11px;");
                    flow.getChildren().add(explText);

                    setGraphic(flow);
                }
            }
        });

        TableColumn<TestCaseDto, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(95);
        actionsCol.setMaxWidth(110);
        actionsCol.setStyle("-fx-alignment: CENTER-LEFT;");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Del");
            private final HBox box = new HBox(4, editBtn, delBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().add("btn-outline");
                editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");

                editBtn.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        TestCaseDto tc = getTableView().getItems().get(getIndex());
                        EditTestCaseDialog.show(tc).ifPresent(updated -> {
                            apiClient.updateTestCase(updated.getId(), updated)
                                    .thenRun(() -> Platform.runLater(() -> {
                                        casesTable.refresh();
                                        runAudit();
                                    }));
                        });
                    }
                });

                delBtn.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        TestCaseDto tc = getTableView().getItems().get(getIndex());
                        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete test case #" + tc.getId() + "?", ButtonType.YES, ButtonType.NO);
                        conf.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.YES) {
                                apiClient.deleteTestCase(tc.getId())
                                        .thenRun(() -> Platform.runLater(() -> {
                                            masterCases.remove(tc);
                                            undoStack.push(tc);
                                            btnUndoDelete.setDisable(false);
                                            btnUndoDelete.setText("\u21A9 Undo (" + undoStack.size() + ")");
                                            updateTabCounts();
                                            updateTableHeight();
                                            runAudit();
                                            updateTableHeight();
                                        }));
                            }
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        casesTable.getColumns().addAll(doneCol, idCol, catCol, prioCol, tagCol, inputCol, outputCol, resultCol, explCol, actionsCol);
        updateTableHeight();
        filteredCases.addListener((ListChangeListener<TestCaseDto>) c -> updateTableHeight());
    }

    private void updateTableHeight() {
        int rows = filteredCases.size();
        if (rows == 0) {
            casesTable.setPrefHeight(75.0);
            casesTable.setMinHeight(75.0);
            casesTable.setMaxHeight(75.0);
            return;
        }
        double targetHeight = Math.min(550.0, 32.0 + (rows * 48.0) + 4.0);
        casesTable.setPrefHeight(targetHeight);
        casesTable.setMinHeight(targetHeight);
        casesTable.setMaxHeight(targetHeight);
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String cat = categoryFilter.getValue();
        String prio = priorityFilter.getValue();
        String rev = reviewFilter.getValue();

        filteredCases.setPredicate(tc -> {
            boolean matchQ = query.isEmpty() ||
                    tc.getInput().toLowerCase().contains(query) ||
                    tc.getExpectedOutput().toLowerCase().contains(query) ||
                    tc.getExplanation().toLowerCase().contains(query) ||
                    tc.getRequirementTag().toLowerCase().contains(query);

            boolean matchCat = "ALL".equals(cat) || tc.getCategory().equalsIgnoreCase(cat);
            boolean matchPrio = "ALL".equals(prio) || tc.getPriority().equalsIgnoreCase(prio);
            boolean matchRev = "ALL".equals(rev) ||
                    ("REVIEWED".equals(rev) && tc.getIsReviewed()) ||
                    ("UNREVIEWED".equals(rev) && !tc.getIsReviewed());

            return matchQ && matchCat && matchPrio && matchRev;
        });
        updateTableHeight();
    }

    // ==========================================
    // Sub-Panel 2: Traceability Matrix (RTM)
    // ==========================================
    private void setupRtmPanel() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox topBox = new HBox(10);
        Label header = new Label("📐 Requirement-to-Test Traceability Matrix (RTM)");
        header.getStyleClass().add("card-header-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rtmCoverageScore.getStyleClass().addAll("badge", "badge-normal");
        rtmCoverageScore.setStyle("-fx-font-size: 13px; -fx-padding: 4px 12px;");

        topBox.getChildren().addAll(header, spacer, rtmCoverageScore);

        Label hint = new Label("Connects decomposed problem requirements to verifying candidate test cases to guarantee full testing coverage.");
        hint.getStyleClass().add("hint-label");

        setupRtmTable();
        card.getChildren().addAll(topBox, hint, rtmTable);
        rtmSubPanel.getChildren().add(card);
    }

    private void setupRtmTable() {
        rtmTable.setItems(rtmData);
        rtmTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Map<String, String>, String> idCol = new TableColumn<>("Req ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id")));
        idCol.setPrefWidth(90);

        TableColumn<Map<String, String>, String> titleCol = new TableColumn<>("Requirement Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("title")));
        titleCol.setPrefWidth(220);

        TableColumn<Map<String, String>, String> descCol = new TableColumn<>("Specification / Condition Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("description")));
        descCol.setPrefWidth(320);

        TableColumn<Map<String, String>, String> casesCol = new TableColumn<>("Mapped Test Cases");
        casesCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("cases")));
        casesCol.setPrefWidth(200);

        TableColumn<Map<String, String>, String> statusCol = new TableColumn<>("Coverage Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status")));
        statusCol.setPrefWidth(160);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label l = new Label(item);
                    if (item.contains("Covered")) l.getStyleClass().addAll("badge", "badge-normal");
                    else l.getStyleClass().addAll("badge", "badge-exception");
                    setGraphic(l);
                }
            }
        });

        rtmTable.getColumns().addAll(idCol, titleCol, descCol, casesCol, statusCol);
        rtmTable.setFixedCellSize(36.0);
        updateRtmTableHeight();
        rtmData.addListener((ListChangeListener<Map<String, String>>) c -> updateRtmTableHeight());
    }

    private void updateRtmTableHeight() {
        int rows = rtmData.size();
        double h = (rows == 0) ? 75.0 : Math.min(500.0, 32.0 + (rows * 36.0) + 4.0);
        rtmTable.setPrefHeight(h);
        rtmTable.setMinHeight(h);
        rtmTable.setMaxHeight(h);
    }

    private void renderRtmData(JsonNode summaryNode) {
        rtmData.clear();
        JsonNode reqs = summaryNode != null ? summaryNode.get("requirements") : null;

        List<Map<String, String>> list = new ArrayList<>();
        if (reqs != null && reqs.isArray() && reqs.size() > 0) {
            for (JsonNode r : reqs) {
                list.add(Map.of(
                        "id", r.path("id").asText("REQ"),
                        "title", r.path("title").asText("Requirement"),
                        "description", r.path("description").asText("Specification condition")
                ));
            }
        } else {
            list.add(Map.of("id", "REQ-01", "title", "Core Functional Logic", "description", "Validate nominal execution and output generation"));
            list.add(Map.of("id", "REQ-02", "title", "Boundary & Constraints", "description", "Test min/max transition edges and constraints"));
            list.add(Map.of("id", "REQ-03", "title", "Defensive & Invalid Inputs", "description", "Gracefully handle malformed or invalid inputs"));
            list.add(Map.of("id", "REQ-04", "title", "Exception Handling", "description", "Prevent unhandled runtime crashes or overflows"));
        }

        // LinkedHashMap preserves requirement mapping order in the RTM table
        LinkedHashMap<String, List<Long>> rtmMapping = new LinkedHashMap<>();

        int covered = 0;
        for (Map<String, String> req : list) {
            String reqId = req.get("id").toUpperCase();
            String reqTitle = req.get("title").toUpperCase();

            // Java Streams for declarative requirement matching
            List<Long> mappedIds = masterCases.stream()
                    .filter(tc -> {
                        String tag = tc.getRequirementTag() != null ? tc.getRequirementTag().toUpperCase() : "";
                        return tag.contains(reqId) || (tag.length() > 3 && reqTitle.contains(tag));
                    })
                    .map(TestCaseDto::getId)
                    .toList();

            if (mappedIds.isEmpty()) {
                // Fallback map by category using Stream
                mappedIds = masterCases.stream()
                        .filter(tc -> {
                            String cat = tc.getCategory();
                            if (reqId.contains("1") && "NORMAL".equalsIgnoreCase(cat)) return true;
                            if (reqId.contains("2") && "BOUNDARY".equalsIgnoreCase(cat)) return true;
                            if (reqId.contains("3") && "INVALID_INPUT".equalsIgnoreCase(cat)) return true;
                            return reqId.contains("4") && ("EXCEPTION".equalsIgnoreCase(cat) || "EDGE".equalsIgnoreCase(cat));
                        })
                        .map(TestCaseDto::getId)
                        .toList();
            }

            rtmMapping.put(req.get("id"), mappedIds);
            boolean isCov = !mappedIds.isEmpty();
            if (isCov) covered++;

            Map<String, String> row = new HashMap<>();
            row.put("id", req.get("id"));
            row.put("title", req.get("title"));
            row.put("description", req.get("description"));
            row.put("cases", isCov ? "Cases: " + mappedIds : "No cases mapped");
            row.put("status", isCov ? "\u2714 Covered (" + mappedIds.size() + " Tests)" : "\u2716 Untested (Gap)");
            rtmData.add(row);
        }

        int percent = list.size() > 0 ? Math.round(((float) covered / list.size()) * 100) : 100;
        rtmCoverageScore.setText(percent + "% Covered (" + covered + "/" + list.size() + ")");
        updateRtmTableHeight();
    }
    // ==========================================
    // Project Loading & Audit Actions
    // ==========================================
    public void loadProject(Long projectId) {
        apiClient.getProject(projectId)
                .thenAccept(data -> Platform.runLater(() -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        currentProject = mapper.treeToValue(data.get("project"), ProblemDto.class);

                        JsonNode casesNode = data.get("testCases");
                        masterCases.clear();
                        if (casesNode != null && casesNode.isArray()) {
                            for (JsonNode c : casesNode) {
                                masterCases.add(mapper.treeToValue(c, TestCaseDto.class));
                            }
                        }

                        titleLabel.setText("📋 " + currentProject.getTitle());
                        metaLabel.setText("Project #" + currentProject.getId() + " · Created " + currentProject.getCreatedAt() + " · Total Cases: " + masterCases.size());
                        qualityScoreBadge.setText("Quality Score: " + currentProject.getQualityScore() + "/100");

                        // Populate Problem Specification & Configuration Details
                        String desc = currentProject.getDescription();
                        if (desc != null && !desc.isBlank()) {
                            problemDescArea.setText(desc);
                            problemSpecCard.setVisible(true);
                            problemSpecCard.setManaged(true);
                        } else {
                            problemDescArea.setText("No problem statement description recorded.");
                            problemSpecCard.setVisible(false);
                            problemSpecCard.setManaged(false);
                        }

                        String settings = currentProject.getSettings();
                        if (settings != null && !settings.isBlank()) {
                            settingsBadge.setText("\u2699 Settings: " + settings);
                            settingsBadge.setVisible(true);
                            settingsBadge.setManaged(true);
                        } else {
                            settingsBadge.setVisible(false);
                            settingsBadge.setManaged(false);
                        }

                        updateTabCounts();
                        updateTableHeight();

                        // Parse coverage summary
                        JsonNode covNode = data.get("project").get("coverageSummary");
                        JsonNode parsedSummary = null;
                        if (covNode != null) {
                            try {
                                if (covNode.isTextual()) parsedSummary = mapper.readTree(covNode.asText());
                                else parsedSummary = covNode;
                            } catch (Exception ignored) {}
                        }

                        // Reqs and Gaps
                        if (parsedSummary != null) {
                            reqsAnalyzedText.setText(parsedSummary.path("requirementsAnalyzed").toString());
                            gapsText.setText(parsedSummary.path("potentialGaps").toString());
                            renderRtmData(parsedSummary);
                        } else {
                            reqsAnalyzedText.setText("Nominal and boundary conditions analyzed.");
                            gapsText.setText("None identified.");
                            renderRtmData(null);
                        }

                        switchSubPanel("cases");
                        runAudit();

                    } catch (Exception e) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Parse Error", null, e.getMessage());
                    }
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Load Failed", null, err.getMessage()));
                    return null;
                });
    }

    private void updateTabCounts() {
        tabCasesBtn.setText("📋 Test Cases Table (" + masterCases.size() + ")");
    }

    private void runAudit() {
        if (masterCases.isEmpty()) return;
        apiClient.auditQuality(masterCases)
                .thenAccept(audit -> Platform.runLater(() -> {
                    this.currentAudit = audit;
                    auditBanner.setVisible(true);
                    auditBanner.setManaged(true);

                    auditIssuesList.getChildren().clear();

                    if ("EXCELLENT".equalsIgnoreCase(audit.getHealthBadge())) {
                        auditBanner.getStyleClass().setAll("card", "audit-banner-clean");
                        auditHealthBadge.setText("Audit: Excellent (100% Clean)");
                        auditSummary.setText("All " + audit.getTotalCases() + " test cases are unique, valid, and free of contradictions.");
                        btnAutoRemoveDuplicates.setVisible(false);
                    } else if ("GOOD".equalsIgnoreCase(audit.getHealthBadge())) {
                        auditBanner.getStyleClass().setAll("card", "audit-banner-clean");
                        auditHealthBadge.setText("Audit: Good");
                        auditSummary.setText("Test suite is valid with minor stylistic notes (" + audit.getFlawCount() + " notes).");
                        btnAutoRemoveDuplicates.setVisible(false);
                    } else {
                        auditBanner.getStyleClass().setAll("card", "audit-banner-warn");
                        auditHealthBadge.setText("CRITICAL".equalsIgnoreCase(audit.getHealthBadge()) ? "Audit: Contradiction Found!" : "Audit: Issues Flagged");
                        auditSummary.setText(audit.getDuplicateCount() + " duplicates/similar, " + audit.getContradictionCount() + " contradictions, " + audit.getFlawCount() + " flaws detected.");
                        btnAutoRemoveDuplicates.setVisible(audit.getDuplicateCount() > 0);
                    }

                    for (String issue : audit.getIssues()) {
                        Label l = new Label("• " + issue);
                        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #fbbf24;");
                        auditIssuesList.getChildren().add(l);
                    }

                    // Map flags back to test case objects
                    if (audit.getCaseFlags() != null) {
                        for (Map<String, Object> flagMap : audit.getCaseFlags()) {
                            int idx = (int) flagMap.get("index");
                            if (idx < masterCases.size()) {
                                @SuppressWarnings("unchecked")
                                List<String> flags = (List<String>) flagMap.get("flags");
                                masterCases.get(idx).setAuditFlags(flags);
                            }
                        }
                    }

                    casesTable.refresh();
                }))
                .exceptionally(err -> null);
    }

    private void regenerateTestCases() {
        if (currentProject == null) return;

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Regenerate test cases for \"" + currentProject.getTitle() + "\"?\nExisting test cases will be replaced with a fresh generated suite.",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Regenerate Test Suite");
        conf.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                btnRegenerate.setDisable(true);
                btnRegenerate.setText("\u21BB Regenerating...");

                Map<String, Object> req = new HashMap<>();
                req.put("problemStatement", currentProject.getDescription());
                req.put("title", currentProject.getTitle());
                req.put("testCount", currentProject.getTestCount() > 0 ? currentProject.getTestCount() : 10);

                List<String> categories = new ArrayList<>();
                String settings = currentProject.getSettings();
                if (settings != null && settings.contains("Categories: ")) {
                    String catsPart = settings.substring(settings.indexOf("Categories: ") + 12).trim();
                    if (!catsPart.isEmpty() && !"ALL".equalsIgnoreCase(catsPart)) {
                        categories.addAll(Arrays.asList(catsPart.split(",")));
                    }
                }
                if (categories.isEmpty()) {
                    categories.addAll(List.of("NORMAL", "BOUNDARY", "EDGE", "INVALID_INPUT", "EXCEPTION"));
                }
                req.put("categories", categories);

                apiClient.regenerateProject(currentProject.getId(), req)
                        .thenAccept(data -> Platform.runLater(() -> {
                            btnRegenerate.setDisable(false);
                            btnRegenerate.setText("\u21BB Regenerate Test Cases");
                            loadProject(currentProject.getId());
                            UIUtils.showAlert(Alert.AlertType.INFORMATION, "Regeneration Complete", null, "Test cases regenerated successfully!");
                        }))
                        .exceptionally(err -> {
                            Platform.runLater(() -> {
                                btnRegenerate.setDisable(false);
                                btnRegenerate.setText("\u21BB Regenerate Test Cases");
                                UIUtils.showAlert(Alert.AlertType.ERROR, "Regeneration Failed", null, "Failed to regenerate: " + err.getMessage());
                            });
                            return null;
                        });
            }
        });
    }

    private void autoRemoveDuplicates() {
        if (currentAudit == null || currentAudit.getCaseFlags() == null) return;
        List<TestCaseDto> toRemove = new ArrayList<>();
        for (Map<String, Object> cf : currentAudit.getCaseFlags()) {
            @SuppressWarnings("unchecked")
            List<String> flags = (List<String>) cf.get("flags");
            if (flags != null && flags.stream().anyMatch(f -> f.contains("Duplicate input") || f.contains("High input similarity"))) {
                int idx = (int) cf.get("index");
                if (idx < masterCases.size()) {
                    toRemove.add(masterCases.get(idx));
                }
            }
        }

        if (toRemove.isEmpty()) return;

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + toRemove.size() + " duplicate test case(s)?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                List<CompletableFuture<Void>> deleteFutures = new ArrayList<>();
                for (TestCaseDto tc : toRemove) {
                    if (tc.getId() != null) {
                        deleteFutures.add(apiClient.deleteTestCase(tc.getId()));
                    }
                }
                CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> Platform.runLater(() -> {
                            masterCases.removeAll(toRemove);
                            for (TestCaseDto tc : toRemove) {
                                undoStack.push(tc);
                            }
                            btnUndoDelete.setDisable(undoStack.isEmpty());
                            btnUndoDelete.setText("\u21A9 Undo (" + undoStack.size() + ")");
                            updateTabCounts();
                            updateTableHeight();
                            runAudit();
                        }))
                        .exceptionally(err -> {
                            Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Delete Failed", null, "Failed to delete duplicates from database: " + err.getMessage()));
                            return null;
                        });
            }
        });
    }
}
