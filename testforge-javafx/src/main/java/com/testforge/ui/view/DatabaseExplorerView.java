package com.testforge.ui.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.testforge.ui.client.ApiClient;
import com.testforge.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class DatabaseExplorerView extends VBox {

    private final ApiClient apiClient;

    private final Label statusBadge = new Label("Connecting...");
    private final Label productLabel = new Label("-");
    private final Label userLabel = new Label("-");
    private final Label urlLabel = new Label("-");

    private final FlowPane tablesPillPane = new FlowPane(8, 8);
    private String selectedTable = "TF_PROBLEMS";

    // Schema Table
    private final VBox schemaCard = new VBox(10);
    private final Label schemaTitle = new Label("📐 Table Columns & Schema: TF_PROBLEMS");
    private final TableView<Map<String, String>> schemaTable = new TableView<>();
    private final ObservableList<Map<String, String>> schemaList = FXCollections.observableArrayList();

    // SQL Console
    private final TextArea sqlEditor = new TextArea();
    private final Label queryStatsLabel = new Label();
    private final Button btnRunQuery = new Button("▶ Run SQL Query");

    // Dynamic Data Grid
    private final Label gridTitle = new Label("📊 Table Records");
    private final Label gridCount = new Label("0 Records");
    private final TableView<Map<String, String>> dataGridTable = new TableView<>();
    private final ObservableList<Map<String, String>> dataGridList = FXCollections.observableArrayList();

    public DatabaseExplorerView(ApiClient apiClient) {
        this.apiClient = apiClient;

        setSpacing(18);
        setPadding(new Insets(24));

        setupConnectionBox();
        setupTablesSelector();
        setupSchemaCard();
        setupSqlConsole();
        setupDataGrid();

        getChildren().addAll(
                buildTopCard(),
                tablesPillPane,
                schemaCard,
                buildConsoleCard(),
                buildGridCard()
        );

        loadDatabaseInfo();
    }

    private VBox buildTopCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox topBox = new HBox(12);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("🗄️ Oracle Database Explorer");
        header.getStyleClass().add("card-header-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBadge.getStyleClass().addAll("badge", "badge-boundary");
        statusBadge.setStyle("-fx-font-size: 12px; -fx-padding: 4px 12px;");

        Button refreshBtn = new Button("🔄 Refresh DB State");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setOnAction(e -> loadDatabaseInfo());

        topBox.getChildren().addAll(header, statusBadge, spacer, refreshBtn);

        // Connection details 3-box
        GridPane connGrid = new GridPane();
        connGrid.setHgap(12);
        connGrid.setVgap(12);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(33.3);
        connGrid.getColumnConstraints().addAll(c, c, c);

        connGrid.add(createConnBox("Database Engine", productLabel), 0, 0);
        connGrid.add(createConnBox("Schema / User", userLabel), 1, 0);
        connGrid.add(createConnBox("JDBC Connection URL", urlLabel), 2, 0);

        card.getChildren().addAll(topBox, connGrid);
        return card;
    }

    private VBox createConnBox(String title, Label valLabel) {
        VBox box = new VBox(4);
        box.getStyleClass().add("card");
        box.setStyle("-fx-background-color: #0b1122; -fx-padding: 10px 14px;");

        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: 700;");

        valLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff; -fx-font-weight: 600;");
        valLabel.setWrapText(true);

        box.getChildren().addAll(t, valLabel);
        return box;
    }

    private void setupConnectionBox() {}
    private void setupTablesSelector() {}

    private void setupSchemaCard() {
        schemaCard.getStyleClass().add("card");
        schemaCard.setStyle("-fx-background-color: #0c1224;");

        schemaTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #818cf8;");

        schemaTable.setItems(schemaList);
        schemaTable.setPrefHeight(160);
        schemaTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Map<String, String>, String> colName = new TableColumn<>("Column Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("name")));

        TableColumn<Map<String, String>, String> colType = new TableColumn<>("Data Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("type")));

        TableColumn<Map<String, String>, String> colLen = new TableColumn<>("Data Length");
        colLen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("length")));

        TableColumn<Map<String, String>, String> colNull = new TableColumn<>("Nullable");
        colNull.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("nullable")));

        schemaTable.getColumns().addAll(colName, colType, colLen, colNull);
        schemaCard.getChildren().addAll(schemaTitle, schemaTable);
    }

    private VBox buildConsoleCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-color: #0f172a;");

        HBox topBox = new HBox(10);
        Label header = new Label("⚡ SQL Query Console (Read-Only)");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #38bdf8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        queryStatsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        topBox.getChildren().addAll(header, spacer, queryStatsLabel);

        // Presets
        HBox presetsBox = new HBox(8);
        presetsBox.getChildren().addAll(
                createPresetBtn("TF_PROBLEMS", "SELECT PROBLEM_ID, TITLE, TEST_COUNT, QUALITY_SCORE, CREATED_AT FROM TF_PROBLEMS ORDER BY PROBLEM_ID DESC"),
                createPresetBtn("TF_TEST_CASES", "SELECT TEST_CASE_ID, PROBLEM_ID, CATEGORY, PRIORITY, IS_REVIEWED, REQUIREMENT_TAG FROM TF_TEST_CASES ORDER BY TEST_CASE_ID DESC"),
                createPresetBtn("Group by Category", "SELECT CATEGORY, COUNT(*) AS TOTAL_CASES FROM TF_TEST_CASES GROUP BY CATEGORY"),
                createPresetBtn("Review Progress", "SELECT COUNT(*) AS TOTAL, NVL(SUM(IS_REVIEWED), 0) AS REVIEWED, COUNT(*) - NVL(SUM(IS_REVIEWED), 0) AS PENDING FROM TF_TEST_CASES")
        );

        sqlEditor.getStyleClass().add("code-editor");
        sqlEditor.setPrefRowCount(3);
        sqlEditor.setPromptText("SELECT * FROM TF_PROBLEMS WHERE QUALITY_SCORE > 80");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnClear = new Button("Clear");
        btnClear.getStyleClass().add("btn-outline");
        btnClear.setOnAction(e -> {
            sqlEditor.clear();
            queryStatsLabel.setText("");
        });

        btnRunQuery.getStyleClass().add("btn-success");
        btnRunQuery.setOnAction(e -> runCustomSql(sqlEditor.getText()));

        actions.getChildren().addAll(btnClear, btnRunQuery);

        card.getChildren().addAll(topBox, presetsBox, sqlEditor, actions);
        return card;
    }

    private Button createPresetBtn(String label, String sql) {
        Button b = new Button(label);
        b.getStyleClass().add("btn-outline");
        b.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
        b.setOnAction(e -> {
            sqlEditor.setText(sql);
            runCustomSql(sql);
        });
        return b;
    }

    private void setupSqlConsole() {}
    private void setupDataGrid() {}

    private VBox buildGridCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox topBox = new HBox(10);
        gridTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        gridCount.getStyleClass().addAll("badge", "badge-normal");
        topBox.getChildren().addAll(gridTitle, spacer, gridCount);

        dataGridTable.setItems(dataGridList);
        dataGridTable.setPrefHeight(320);
        VBox.setVgrow(dataGridTable, Priority.ALWAYS);

        card.getChildren().addAll(topBox, dataGridTable);
        return card;
    }

    private void loadDatabaseInfo() {
        statusBadge.setText("Connecting...");
        apiClient.getDatabaseStatus()
                .thenAccept(data -> Platform.runLater(() -> {
                    if (data.path("connected").asBoolean(false)) {
                        statusBadge.setText("🟢 Online");
                        statusBadge.getStyleClass().setAll("badge", "badge-normal");
                        productLabel.setText(data.path("databaseVersion").asText("Oracle 11g"));
                        userLabel.setText(data.path("userName").asText("SCOTT"));
                        urlLabel.setText(data.path("url").asText("-"));
                    } else {
                        statusBadge.setText("🔴 Disconnected");
                        statusBadge.getStyleClass().setAll("badge", "badge-exception");
                    }
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> {
                        statusBadge.setText("🔴 Offline");
                        statusBadge.getStyleClass().setAll("badge", "badge-exception");
                    });
                    return null;
                });

        loadTables();
    }

    private void loadTables() {
        apiClient.getDatabaseTables()
                .thenAccept(arr -> Platform.runLater(() -> {
                    tablesPillPane.getChildren().clear();
                    if (arr.isArray()) {
                        for (JsonNode t : arr) {
                            String name = t.path("tableName").asText();
                            long count = t.path("rowCount").asLong(0);

                            Button pill = new Button(name + " (" + count + ")");
                            pill.getStyleClass().add("pill-btn");
                            if (name.equalsIgnoreCase(selectedTable)) pill.getStyleClass().add("pill-btn-active");

                            pill.setOnAction(e -> {
                                selectedTable = name;
                                tablesPillPane.getChildren().forEach(n -> n.getStyleClass().remove("pill-btn-active"));
                                pill.getStyleClass().add("pill-btn-active");
                                loadTableData(name);
                            });

                            tablesPillPane.getChildren().add(pill);
                        }
                    }

                    loadTableData(selectedTable);
                }))
                .exceptionally(err -> null);
    }

    private void loadTableData(String table) {
        gridTitle.setText("📊 Table: " + table);
        gridCount.setText("Loading...");

        apiClient.getTableData(table)
                .thenAccept(res -> Platform.runLater(() -> {
                    // 1. Schema
                    schemaTitle.setText("📐 Columns & Schema: " + table);
                    schemaList.clear();
                    JsonNode cols = res.get("columns");
                    List<String> colNames = new ArrayList<>();
                    if (cols != null && cols.isArray()) {
                        for (JsonNode c : cols) {
                            String colName = c.path("name").asText();
                            colNames.add(colName);
                            schemaList.add(Map.of(
                                    "name", colName,
                                    "type", c.path("type").asText(),
                                    "length", c.path("length").asText(),
                                    "nullable", "Y".equalsIgnoreCase(c.path("nullable").asText()) ? "YES" : "NO (Required)"
                            ));
                        }
                    }

                    // 2. Data Grid
                    JsonNode rows = res.get("rows");
                    renderDynamicGrid(colNames, rows);
                    gridCount.setText(res.path("totalFetched").asInt(0) + " Rows Loaded");
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Table Load Error", null, err.getMessage()));
                    return null;
                });
    }

    private void runCustomSql(String sql) {
        if (sql == null || sql.isBlank()) return;
        queryStatsLabel.setText("⚡ Executing query...");

        apiClient.runSqlQuery(sql)
                .thenAccept(res -> Platform.runLater(() -> {
                    queryStatsLabel.setText("⚡ Executed in " + res.path("executionTimeMs").asLong(0) + "ms · " + res.path("rowCount").asInt(0) + " rows");
                    gridTitle.setText("📊 Custom Query Result");
                    gridCount.setText(res.path("rowCount").asInt(0) + " Rows");

                    List<String> cols = new ArrayList<>();
                    JsonNode colsNode = res.get("columns");
                    if (colsNode != null && colsNode.isArray()) {
                        for (JsonNode c : colsNode) cols.add(c.asText());
                    }

                    renderDynamicGrid(cols, res.get("rows"));
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> {
                        queryStatsLabel.setText("❌ Error executing SQL");
                        UIUtils.showAlert(Alert.AlertType.ERROR, "SQL Error", null, err.getMessage());
                    });
                    return null;
                });
    }

    private void renderDynamicGrid(List<String> columnNames, JsonNode rowsNode) {
        dataGridTable.getColumns().clear();
        dataGridList.clear();

        if (columnNames == null || columnNames.isEmpty()) return;

        for (String colName : columnNames) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(colName);
            col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(colName)));
            col.setPrefWidth(140);
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label l = new Label(item);
                        l.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
                        setGraphic(l);
                    }
                }
            });
            dataGridTable.getColumns().add(col);
        }

        if (rowsNode != null && rowsNode.isArray()) {
            for (JsonNode r : rowsNode) {
                Map<String, String> rowMap = new HashMap<>();
                for (String c : columnNames) {
                    JsonNode v = r.get(c);
                    rowMap.put(c, v != null && !v.isNull() ? v.asText() : "NULL");
                }
                dataGridList.add(rowMap);
            }
        }
    }
}
