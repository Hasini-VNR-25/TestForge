package com.testforge.ui.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.testforge.ui.client.ApiClient;
import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class DashboardView extends VBox {

    private final ApiClient apiClient;
    private final Consumer<Long> onOpenProject;

    private final Label kpiProblems = new Label("0");
    private final Label kpiTestCases = new Label("0");
    private final Label kpiReviewed = new Label("0");
    private final Label kpiScore = new Label("0%");
    private final Label kpiReviewedSub = new Label("0% coverage verified");

    private final Label catNormal = new Label("0");
    private final Label catBoundary = new Label("0");
    private final Label catEdge = new Label("0");
    private final Label catInvalid = new Label("0");
    private final Label catException = new Label("0");

    private final TableView<ProblemDto> recentTable = new TableView<>();
    private final ObservableList<ProblemDto> recentList = FXCollections.observableArrayList();

    public DashboardView(ApiClient apiClient, Consumer<Long> onOpenProject) {
        this.apiClient = apiClient;
        this.onOpenProject = onOpenProject;

        setSpacing(20);
        setPadding(new Insets(24));
        getStyleClass().add("dashboard-view");

        // 1. KPI Cards Grid
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(25);
        kpiGrid.getColumnConstraints().addAll(col, col, col, col);

        VBox c1 = UIUtils.createKpiCard("Problems Analyzed", kpiProblems, "Total problem statements saved", "info");
        VBox c2 = UIUtils.createKpiCard("Test Cases Generated", kpiTestCases, "Across normal & edge categories", null);
        VBox c3 = UIUtils.createKpiCard("Reviewed Test Cases", kpiReviewed, "0% verified", "success");
        VBox c4 = UIUtils.createKpiCard("Avg Quality Score", kpiScore, "AI coverage completeness score", "warning");

        kpiGrid.add(c1, 0, 0);
        kpiGrid.add(c2, 1, 0);
        kpiGrid.add(c3, 2, 0);
        kpiGrid.add(c4, 3, 0);

        // 2. Global Test Coverage by Category Card
        VBox catCard = new VBox(10);
        catCard.getStyleClass().add("card");

        Label catHeader = new Label("🏷️ Global Test Coverage by Category");
        catHeader.getStyleClass().add("card-header-label");

        Label catHint = new Label("Cumulative distribution of all candidate test cases generated in Oracle Database.");
        catHint.getStyleClass().add("hint-label");

        HBox pillsBox = new HBox(12);
        pillsBox.getChildren().addAll(
                createCatPill("Normal / Positive", catNormal, "badge-normal"),
                createCatPill("Boundary Values", catBoundary, "badge-boundary"),
                createCatPill("Edge Cases", catEdge, "badge-edge"),
                createCatPill("Invalid Inputs", catInvalid, "badge-invalid"),
                createCatPill("Exceptions & Errors", catException, "badge-exception")
        );

        catCard.getChildren().addAll(catHeader, catHint, pillsBox);

        // 3. Recent Projects Card
        VBox recentCard = new VBox(12);
        recentCard.getStyleClass().add("card");
        VBox.setVgrow(recentCard, Priority.ALWAYS);

        HBox recentHeaderBox = new HBox(10);
        Label recentHeader = new Label("📂 Recent Problems & Test Suites");
        recentHeader.getStyleClass().add("card-header-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh Stats");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setOnAction(e -> loadData());

        recentHeaderBox.getChildren().addAll(recentHeader, spacer, refreshBtn);

        setupRecentTable();
        recentCard.getChildren().addAll(recentHeaderBox, recentTable);

        getChildren().addAll(kpiGrid, catCard, recentCard);

        loadData();
    }

    private HBox createCatPill(String title, Label countLabel, String badgeClass) {
        HBox pill = new HBox(8);
        pill.getStyleClass().add("card");
        pill.setPadding(new Insets(8, 14, 8, 14));
        pill.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(title);
        badge.getStyleClass().addAll("badge", badgeClass);

        countLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #ffffff; -fx-font-size: 13px;");
        pill.getChildren().addAll(badge, countLabel);
        return pill;
    }

    private void setupRecentTable() {
        recentTable.setItems(recentList);
        recentTable.setPlaceholder(new Label("No projects found in Oracle. Click 'New Generator' to start!"));
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ProblemDto, String> idCol = new TableColumn<>("# ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        idCol.setPrefWidth(60);

        TableColumn<ProblemDto, String> titleCol = new TableColumn<>("Problem Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setPrefWidth(260);

        TableColumn<ProblemDto, String> dateCol = new TableColumn<>("Created Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt() : "-"));
        dateCol.setPrefWidth(160);

        TableColumn<ProblemDto, String> casesCol = new TableColumn<>("Total Cases");
        casesCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTestCount() + " cases"));
        casesCol.setPrefWidth(110);
        casesCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label l = new Label(item);
                    l.getStyleClass().addAll("badge", "badge-boundary");
                    setGraphic(l);
                }
            }
        });

        TableColumn<ProblemDto, String> scoreCol = new TableColumn<>("Quality Score");
        scoreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getQualityScore() + "/100"));
        scoreCol.setPrefWidth(110);
        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label l = new Label(item);
                    l.getStyleClass().addAll("badge", "badge-normal");
                    setGraphic(l);
                }
            }
        });

        TableColumn<ProblemDto, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setStyle("-fx-alignment: CENTER-LEFT;");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(6, openBtn, delBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                openBtn.getStyleClass().add("btn-primary");
                openBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
                delBtn.getStyleClass().add("btn-danger");

                openBtn.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ProblemDto p = getTableView().getItems().get(getIndex());
                        onOpenProject.accept(p.getId());
                    }
                });

                delBtn.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ProblemDto p = getTableView().getItems().get(getIndex());
                        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete project #" + p.getId() + " and its test cases?", ButtonType.YES, ButtonType.NO);
                        conf.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.YES) {
                                apiClient.deleteProject(p.getId())
                                        .thenRun(() -> Platform.runLater(DashboardView.this::loadData));
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

        recentTable.getColumns().addAll(idCol, titleCol, dateCol, casesCol, scoreCol, actionsCol);
    }

    public void loadData() {
        apiClient.getDashboard()
                .thenAccept(data -> Platform.runLater(() -> {
                    kpiProblems.setText(String.valueOf(data.path("totalProblems").asLong(0)));
                    kpiTestCases.setText(String.valueOf(data.path("totalTestCases").asLong(0)));
                    long reviewed = data.path("reviewedTestCases").asLong(0);
                    long total = data.path("totalTestCases").asLong(0);
                    kpiReviewed.setText(String.valueOf(reviewed));
                    int percent = total > 0 ? (int) Math.round(((double) reviewed / total) * 100) : 0;
                    kpiReviewedSub.setText(percent + "% test coverage verified");

                    int avgScore = data.path("averageQualityScore").asInt(0);
                    int topScore = data.path("topQualityScore").asInt(0);
                    if (topScore > 0) {
                        kpiScore.setText(avgScore + "% (Peak " + topScore + "%)");
                    } else {
                        kpiScore.setText(avgScore + "%");
                    }

                    JsonNode cats = data.path("categoryCounts");
                    catNormal.setText(String.valueOf(cats.path("NORMAL").asInt(0)));
                    catBoundary.setText(String.valueOf(cats.path("BOUNDARY").asInt(0)));
                    catEdge.setText(String.valueOf(cats.path("EDGE").asInt(0)));
                    catInvalid.setText(String.valueOf(cats.path("INVALID_INPUT").asInt(0)));
                    catException.setText(String.valueOf(cats.path("EXCEPTION").asInt(0)));

                    recentList.clear();
                    JsonNode recent = data.path("recentProjects");
                    if (recent.isArray()) {
                        for (JsonNode p : recent) {
                            ProblemDto dto = new ProblemDto();
                            dto.setId(p.path("id").asLong());
                            dto.setTitle(p.path("title").asText("Untitled"));
                            dto.setCreatedAt(p.path("createdAt").asText("-"));
                            dto.setTestCount(p.path("testCount").asInt(0));
                            dto.setQualityScore(p.path("qualityScore").asInt(0));
                            recentList.add(dto);
                        }
                    }
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Dashboard Load Error", null, err.getMessage()));
                    return null;
                });
    }
}
