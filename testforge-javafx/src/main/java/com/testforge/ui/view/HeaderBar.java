package com.testforge.ui.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HeaderBar extends HBox {

    private final Button dashboardBtn;
    private final Button generatorBtn;
    private final Button projectsBtn;
    private final Button managerBtn;
    private final Button dbExplorerBtn;
    private final Map<String, Button> buttonMap = new HashMap<>();
    private Button activeBtn = null;

    public HeaderBar(Consumer<String> onNavigate) {
        getStyleClass().add("header-bar");
        setSpacing(14);
        setAlignment(Pos.CENTER_LEFT);

        // Brand
        Label brand = new Label("⚡ TestForge AI");
        brand.getStyleClass().add("brand-title");

        Label badge = new Label("v2.0 · Enterprise");
        badge.getStyleClass().add("brand-subtitle");

        HBox brandBox = new HBox(10, brand, badge);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Nav Buttons
        dashboardBtn = createNavBtn("📊 Dashboard", "dashboard", onNavigate);
        generatorBtn = createNavBtn("⚡ New Generator", "generator", onNavigate);
        projectsBtn = createNavBtn("📁 Projects & History", "projects", onNavigate);
        managerBtn = createNavBtn("📋 Test Case Manager", "manager", onNavigate);
        dbExplorerBtn = createNavBtn("🗄️ DB Explorer", "database", onNavigate);

        managerBtn.setVisible(false);
        managerBtn.setManaged(false);

        buttonMap.put("dashboard", dashboardBtn);
        buttonMap.put("generator", generatorBtn);
        buttonMap.put("projects", projectsBtn);
        buttonMap.put("manager", managerBtn);
        buttonMap.put("database", dbExplorerBtn);

        HBox navBox = new HBox(8, dashboardBtn, generatorBtn, projectsBtn, managerBtn, dbExplorerBtn);
        navBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(brandBox, spacer, navBox);

        setActive("dashboard");
    }

    private Button createNavBtn(String text, String viewKey, Consumer<String> onNavigate) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> {
            setActive(viewKey);
            onNavigate.accept(viewKey);
        });
        return btn;
    }

    public void setActive(String viewKey) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
        }
        Button btn = buttonMap.get(viewKey);
        if (btn != null) {
            if (!btn.getStyleClass().contains("nav-button-active")) {
                btn.getStyleClass().add("nav-button-active");
            }
            activeBtn = btn;
        }
    }

    public void showManagerButton(boolean show) {
        managerBtn.setVisible(show);
        managerBtn.setManaged(show);
    }
}
