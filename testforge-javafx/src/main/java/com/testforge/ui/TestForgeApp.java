package com.testforge.ui;

import com.testforge.ui.client.ApiClient;
import com.testforge.ui.view.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class TestForgeApp extends Application {

    private ApiClient apiClient;
    private HeaderBar headerBar;
    private ScrollPane contentScrollPane;

    private DashboardView dashboardView;
    private GeneratorView generatorView;
    private ProjectsView projectsView;
    private ManagerView managerView;
    private DatabaseExplorerView databaseExplorerView;

    private final Map<String, javafx.scene.Node> views = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        apiClient = new ApiClient();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Views
        dashboardView = new DashboardView(apiClient, this::openProjectInManager);
        generatorView = new GeneratorView(apiClient, this::openProjectInManager);
        projectsView = new ProjectsView(apiClient, this::openProjectInManager);
        managerView = new ManagerView(apiClient, primaryStage);
        databaseExplorerView = new DatabaseExplorerView(apiClient);

        views.put("dashboard", dashboardView);
        views.put("generator", generatorView);
        views.put("projects", projectsView);
        views.put("manager", managerView);
        views.put("database", databaseExplorerView);

        // Header Navigation
        headerBar = new HeaderBar(this::navigateTo);
        root.setTop(headerBar);

        // Content ScrollPane
        contentScrollPane = new ScrollPane();
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setFitToHeight(false);
        contentScrollPane.getStyleClass().add("scroll-pane");
        root.setCenter(contentScrollPane);

        // Default view
        navigateTo("dashboard");

        Scene scene = new Scene(root, 1280, 850);
        String css = getClass().getResource("/styles/style.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("TestForge AI - Enterprise Test Generation & Quality Studio");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private void navigateTo(String viewKey) {
        javafx.scene.Node node = views.get(viewKey);
        if (node != null) {
            contentScrollPane.setContent(node);
            contentScrollPane.setVvalue(0.0);
            headerBar.setActive(viewKey);

            if ("dashboard".equals(viewKey)) {
                dashboardView.loadData();
            } else if ("projects".equals(viewKey)) {
                projectsView.loadProjects();
            }
        }
    }

    private void openProjectInManager(Long projectId) {
        headerBar.showManagerButton(true);
        managerView.loadProject(projectId);
        navigateTo("manager");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
