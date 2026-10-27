package org.example.View;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.theme.ThemeManager;
import org.example.theme.ThemeManager.Tema;

import java.util.function.Consumer;

public final class WindowBar {

    private static final String ICONE_ESCURO = "🌙";
    private static final String ICONE_CLARO = "☀️";

    private WindowBar() {
    }

    public static HBox criarBarraSuperior(Stage stage, String tituloJanela, Runnable onTemaAlterado) {
        HBox topBar = new HBox(8);
        topBar.setPadding(new Insets(8, 12, 8, 16));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label(tituloJanela);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnTema = criarBotao(iconeDoTema(), false);
        btnTema.setTooltip(new Tooltip("Alternar tema"));

        Button btnMinimizar = criarBotao("–", false);
        btnMinimizar.setTooltip(new Tooltip("Minimizar"));
        btnMinimizar.setOnAction(e -> stage.setIconified(true));

        Button btnFechar = criarBotao("✕", true);
        btnFechar.setTooltip(new Tooltip("Fechar"));
        btnFechar.setOnAction(e -> stage.close());

        HBox controles = new HBox(4, btnTema, btnMinimizar, btnFechar);
        controles.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(lblTitulo, spacer, controles);

        // Reaplica os estilos que dependem do tema (a cor dos botões vem do hover instalado).
        Runnable aplicarTema = () -> {
            topBar.setStyle(ThemeManager.getTopBarStyle());
            lblTitulo.setStyle(ThemeManager.getTituloBarraStyle());
            btnTema.setText(iconeDoTema());
            btnTema.setStyle(ThemeManager.getBotaoJanelaStyle(false, false));
            btnMinimizar.setStyle(ThemeManager.getBotaoJanelaStyle(false, false));
            btnFechar.setStyle(ThemeManager.getBotaoJanelaStyle(false, true));
        };
        aplicarTema.run();

        btnTema.setOnAction(e -> {
            ThemeManager.alternarTema();
            if (onTemaAlterado != null) {
                onTemaAlterado.run();
            }
        });

        // O tema também pode mudar fora da barra; ela acompanha e se desregistra ao fechar.
        Consumer<Tema> ouvinte = tema -> aplicarTema.run();
        ThemeManager.aoMudarTema(ouvinte);
        ChangeListener<Boolean> aoFechar = (obs, antes, visivel) -> {
            if (!visivel) {
                ThemeManager.removerOuvinte(ouvinte);
            }
        };
        stage.showingProperty().addListener(aoFechar);

        habilitarArraste(topBar, stage);
        return topBar;
    }

    private static Button criarBotao(String texto, boolean fechar) {
        Button botao = new Button(texto);
        botao.setFocusTraversable(false);
        botao.setStyle(ThemeManager.getBotaoJanelaStyle(false, fechar));
        botao.setOnMouseEntered(e -> botao.setStyle(ThemeManager.getBotaoJanelaStyle(true, fechar)));
        botao.setOnMouseExited(e -> botao.setStyle(ThemeManager.getBotaoJanelaStyle(false, fechar)));
        return botao;
    }

    private static String iconeDoTema() {
        return ThemeManager.getTemaAtual() == Tema.ESCURO ? ICONE_ESCURO : ICONE_CLARO;
    }

    /** Arrastar a barra move a janela; clique duplo alterna maximizar. */
    private static void habilitarArraste(HBox barra, Stage stage) {
        double[] deslocamento = new double[2];

        barra.setOnMousePressed(e -> {
            deslocamento[0] = e.getSceneX();
            deslocamento[1] = e.getSceneY();
        });

        barra.setOnMouseDragged(e -> {
            if (stage.isMaximized()) {
                return;
            }
            stage.setX(e.getScreenX() - deslocamento[0]);
            stage.setY(e.getScreenY() - deslocamento[1]);
        });

        barra.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }
}