package com.testforge.ui.view;

import com.testforge.ui.client.ApiClient;
import com.testforge.ui.model.ProblemDto;
import com.testforge.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

public class ProjectsView extends VBox {

    private final ApiClient apiClient;
    private final Consumer<Long> onOpenProject;

    private final ObservableList<ProblemDto> masterData = FXCollections.observableArrayList();
    private final FilteredList<ProblemDto> filteredData = new FilteredList<>(masterData, p -> true);
    private final TableView<ProblemDto> table = new TableView<>();
    private final TextField searchField = new TextField();

    public ProjectsView(ApiClient apiClient, Consumer<Long> onOpenProject) {
        this.apiClient = apiClient;
        this.onOpenProject = onOpenProject;

        setSpacing(18);
        setPadding(new Insets(24));

        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox headerBox = new HBox(10);
        Label header = new Label("📁 Problem Statements & Project History");
        header.getStyleClass().add("card-header-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh List");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setOnAction(e -> loadProjects());

        headerBox.getChildren().addAll(header, spacer, refreshBtn);

        searchField.setPromptText("🔍 Search past projects by title, requirement, or description...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(p -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return (p.getTitle() != null && p.getTitle().toLowerCase().contains(lower)) ||
                       (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower));
            });
        });

        setupTable();
        card.getChildren().addAll(headerBox, searchField, table);

        getChildren().add(card);
        loadProjects();
    }

    private void setupTable() {
        table.setItems(filteredData);
        table.setPlaceholder(new Label("No projects found in Oracle Database."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ProblemDto, String> idCol = new TableColumn<>("# ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        idCol.setPrefWidth(60);

        TableColumn<ProblemDto, String> titleCol = new TableColumn<>("Problem Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setPrefWidth(260);

        TableColumn<ProblemDto, String> dateCol = new TableColumn<>("Saved Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt() : "-"));
        dateCol.setPrefWidth(160);

        TableColumn<ProblemDto, String> casesCol = new TableColumn<>("Test Count");
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

        TableColumn<ProblemDto, String> settingsCol = new TableColumn<>("Settings");
        settingsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSettings() != null ? d.getValue().getSettings() : "-"));
        settingsCol.setPrefWidth(200);

        TableColumn<ProblemDto, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(160);
        actionsCol.setStyle("-fx-alignment: CENTER-LEFT;");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open Manager");
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
                        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete Project #" + p.getId() + " from Oracle DB?", ButtonType.YES, ButtonType.NO);
                        conf.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.YES) {
                                apiClient.deleteProject(p.getId())
                                        .thenRun(() -> Platform.runLater(ProjectsView.this::loadProjects));
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

        table.getColumns().addAll(idCol, titleCol, dateCol, casesCol, scoreCol, settingsCol, actionsCol);
    }

    public void loadProjects() {
        apiClient.getProjects()
                .thenAccept(list -> Platform.runLater(() -> {
                    masterData.setAll(list);
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Load Error", null, err.getMessage()));
                    return null;
                });
    }
}
