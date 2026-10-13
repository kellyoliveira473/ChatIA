package org.example.View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Usuario;

public class ChatView {

    private final Usuario usuario;
    private StackPane areaCentral;
    private VBox chatContainer;
    private HBox inputBar;

    public ChatView(Usuario usuario) {
        this.usuario = usuario;
    }

    public void exibir(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;");

        // --- PAINEL LATERAL ESQUERDO (SIDEBAR) ---
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #1e293b; -fx-min-width: 230px;");

        Label lblUser = new Label("Olá, " + (usuario != null && usuario.getNome() != null ? usuario.getNome() : "Usuário"));
        lblUser.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 0 0 10 0;");

        // Botões do Menu (com caracteres limpos para evitar '')
        Button btnNovaConversa = criarBotaoMenu("Nova conversa", true);
        Button btnBiblioteca = criarBotaoMenu("Biblioteca", false);
        Button btnImagens = criarBotaoMenu("Imagens", false);
        Button btnPersonalizar = criarBotaoMenu("Personalizar", false);
        Button btnConfiguracoes = criarBotaoMenu("Configurações", false);

        sidebar.getChildren().addAll(
                lblUser,
                btnNovaConversa,
                btnBiblioteca,
                btnImagens,
                btnPersonalizar,
                btnConfiguracoes
        );
        root.setLeft(sidebar);

        // --- ÁREA CENTRAL DINÂMICA ---
        areaCentral = new StackPane();
        root.setCenter(areaCentral);

        // Layout do Chat
        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(chatContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        adicionarBalao(chatContainer, "Olá! Sou a Luna IA. Como posso te ajudar hoje?", false);

        // Barra de Entrada de Texto
        inputBar = new HBox(10);
        inputBar.setPadding(new Insets(15));
        inputBar.setStyle("-fx-background-color: #1e293b;");
        inputBar.setAlignment(Pos.CENTER);

        TextField txtMensagem = new TextField();
        txtMensagem.setPromptText("Digite sua mensagem...");
        txtMensagem.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-prompt-text-fill: #94a3b8; -fx-font-size: 14px; -fx-background-radius: 8px; -fx-padding: 10px;");
        HBox.setHgrow(txtMensagem, Priority.ALWAYS);

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8px; -fx-padding: 10px 18px;");

        btnEnviar.setOnAction(e -> {
            String texto = txtMensagem.getText().trim();
            if (!texto.isEmpty()) {
                adicionarBalao(chatContainer, texto, true);
                txtMensagem.clear();
                adicionarBalao(chatContainer, "Recebi sua mensagem: \"" + texto + "\".", false);
            }
        });

        txtMensagem.setOnAction(e -> btnEnviar.fire());
        inputBar.getChildren().addAll(txtMensagem, btnEnviar);

        BorderPane layoutChat = new BorderPane();
        layoutChat.setCenter(scrollPane);
        layoutChat.setBottom(inputBar);

        // Exibe o chat como tela padrão
        areaCentral.getChildren().add(layoutChat);

        // --- CONFIGURAÇÃO DOS CLIQUES DOS BOTÕES DO MENU ---
        btnNovaConversa.setOnAction(e -> {
            destacarBotao(btnNovaConversa, btnBiblioteca, btnImagens, btnPersonalizar, btnConfiguracoes);
            chatContainer.getChildren().clear();
            adicionarBalao(chatContainer, "Nova conversa iniciada! Como posso ajudar?", false);
            areaCentral.getChildren().setAll(layoutChat);
        });

        btnBiblioteca.setOnAction(e -> {
            destacarBotao(btnBiblioteca, btnNovaConversa, btnImagens, btnPersonalizar, btnConfiguracoes);
            areaCentral.getChildren().setAll(criarTelaGenerica("Biblioteca", "Aqui você poderá ver seu histórico e conversas salvas."));
        });

        btnImagens.setOnAction(e -> {
            destacarBotao(btnImagens, btnNovaConversa, btnBiblioteca, btnPersonalizar, btnConfiguracoes);
            areaCentral.getChildren().setAll(criarTelaGenerica("Imagens", "Geração e galeria de imagens da Luna IA."));
        });

        btnPersonalizar.setOnAction(e -> {
            destacarBotao(btnPersonalizar, btnNovaConversa, btnBiblioteca, btnImagens, btnConfiguracoes);
            areaCentral.getChildren().setAll(criarTelaGenerica("Personalizar", "Ajuste o tom, avatar e comportamento da Luna IA."));
        });

        btnConfiguracoes.setOnAction(e -> {
            destacarBotao(btnConfiguracoes, btnNovaConversa, btnBiblioteca, btnImagens, btnPersonalizar);
            areaCentral.getChildren().setAll(criarTelaGenerica("Configurações", "Configurações de conta, temas e dados do perfil."));
        });

        // Configuração da Janela
        Scene scene = new Scene(root, 950, 620);
        stage.setTitle("Luna IA - Chat");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        stage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
    }

    private Button criarBotaoMenu(String texto, boolean destacado) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        aplicarEstiloBotao(btn, destacado);
        return btn;
    }

    private void aplicarEstiloBotao(Button btn, boolean destacado) {
        if (destacado) {
            btn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px;"));
        }
    }

    private void destacarBotao(Button ativo, Button... outros) {
        aplicarEstiloBotao(ativo, true);
        for (Button b : outros) {
            aplicarEstiloBotao(b, false);
        }
    }

    private VBox criarTelaGenerica(String titulo, String descricao) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label lblDesc = new Label(descricao);
        lblDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        box.getChildren().addAll(lblTitulo, lblDesc);
        return box;
    }

    private void adicionarBalao(VBox container, String texto, boolean isUsuario) {
        Label label = new Label(texto);
        label.setWrapText(true);
        label.setMaxWidth(450);

        HBox wrapper = new HBox(label);
        wrapper.setPadding(new Insets(4));

        if (isUsuario) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            label.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-padding: 10px 16px; -fx-background-radius: 16px 16px 2px 16px; -fx-font-size: 14px;");
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            label.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-padding: 10px 16px; -fx-background-radius: 16px 16px 16px 2px; -fx-font-size: 14px;");
        }

        container.getChildren().add(wrapper);
    }
}