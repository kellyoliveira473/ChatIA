package org.example.View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class WindowBar {

    private static double xOffset = 0;
    private static double yOffset = 0;

    public static HBox criarBarraSuperior(Stage stage, String titulo) {
        HBox topWindowBar = new HBox();
        topWindowBar.setPadding(new Insets(6, 12, 6, 15));
        topWindowBar.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        topWindowBar.setAlignment(Pos.CENTER_LEFT);

        topWindowBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        topWindowBar.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        Label lblAppName = new Label(titulo);
        lblAppName.setStyle("-fx-text-fill: #a855f7; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        // Botão Minimizar
        Button btnMinimizar = new Button("—");
        btnMinimizar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
        btnMinimizar.setOnAction(e -> stage.setIconified(true));

        // Botão Maximizar / Restaurar
        Button btnMaximizar = new Button("▢");
        btnMaximizar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
        btnMaximizar.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        // Botão Fechar
        Button btnFechar = new Button("✕");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
        btnFechar.setOnAction(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        topWindowBar.getChildren().addAll(lblAppName, topSpacer, btnMinimizar, btnMaximizar, btnFechar);
        return topWindowBar;
    }
}