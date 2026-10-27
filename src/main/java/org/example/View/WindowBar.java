package org.example.View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.example.theme.ThemeManager;
import org.example.theme.ThemeManager.Tema;

public class WindowBar {

    private static boolean temaEscuroGlobal = true;

    public static HBox criarBarraSuperior(Stage stage, String tituloJanela, Runnable onTemaAlterado) {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(8, 12, 8, 12));
        topBar.setAlignment(Pos.CENTER_LEFT);

        atualizarEstiloBarra(topBar);

        Label lblTitulo = new Label(tituloJanela);
        lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnTemaGlobal = new Button(temaEscuroGlobal ? "🌙" : "☀️");
        btnTemaGlobal.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 2px 6px;");
        btnTemaGlobal.setTooltip(new Tooltip("Alternar Tema"));

        btnTemaGlobal.setOnAction(e -> {
            temaEscuroGlobal = !temaEscuroGlobal;
            if (temaEscuroGlobal) {
                ThemeManager.setTemaAtual(Tema.ESCURO);
                btnTemaGlobal.setText("🌙");
            } else {
                ThemeManager.setTemaAtual(Tema.CLARO);
                btnTemaGlobal.setText("☀️");
            }

            atualizarEstiloBarra(topBar);
            lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-weight: bold; -fx-font-size: 13px;");

            if (onTemaAlterado != null) {
                onTemaAlterado.run();
            }
        });

        Button btnMinimizar = new Button("🗕");
        btnMinimizar.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-cursor: hand;");
        btnMinimizar.setOnAction(e -> stage.setIconified(true));

        Button btnFechar = new Button("✕");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> stage.close());

        HBox windowControls = new HBox(5, btnTemaGlobal, btnMinimizar, btnFechar);
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(lblTitulo, spacer, windowControls);
        return topBar;
    }

    private static void atualizarEstiloBarra(HBox bar) {
        if (ThemeManager.getTemaAtual() == Tema.CLARO) {
            bar.setStyle("-fx-background-color: #cbd5e1; -fx-border-color: #94a3b8; -fx-border-width: 0 0 1px 0;");
        } else {
            bar.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 0 0 1px 0;");
        }
    }
}