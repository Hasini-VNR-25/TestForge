package com.testforge.ui.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class UIUtils {

    public static Label createCategoryBadge(String category) {
        String cat = (category != null ? category : "NORMAL").toUpperCase();
        Label badge = new Label(cat);
        badge.getStyleClass().add("badge");

        switch (cat) {
            case "NORMAL" -> badge.getStyleClass().add("badge-normal");
            case "BOUNDARY" -> badge.getStyleClass().add("badge-boundary");
            case "EDGE" -> badge.getStyleClass().add("badge-edge");
            case "INVALID_INPUT" -> badge.getStyleClass().add("badge-invalid");
            case "EXCEPTION" -> badge.getStyleClass().add("badge-exception");
            default -> badge.getStyleClass().add("badge-normal");
        }
        return badge;
    }

    public static Label createPriorityBadge(String priority) {
        String p = (priority != null ? priority : "MEDIUM").toUpperCase();
        Label badge = new Label(p);
        badge.getStyleClass().add("badge");

        switch (p) {
            case "HIGH" -> badge.getStyleClass().add("badge-exception");
            case "MEDIUM" -> badge.getStyleClass().add("badge-invalid");
            case "LOW" -> badge.getStyleClass().add("badge-boundary");
            default -> badge.getStyleClass().add("badge-boundary");
        }
        return badge;
    }

    public static VBox createKpiCard(String title, Label valueLabel, String subtitle, String cardType) {
        VBox card = new VBox(6);
        card.getStyleClass().add("kpi-card");
        if (cardType != null) {
            card.getStyleClass().add("kpi-card-" + cardType);
        }

        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.getStyleClass().add("kpi-title");

        valueLabel.getStyleClass().add("kpi-value");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("kpi-subtitle");

        card.getChildren().addAll(titleLbl, valueLabel, subLbl);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
