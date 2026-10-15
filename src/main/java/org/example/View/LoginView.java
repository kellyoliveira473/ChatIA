package org.example.View;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.controlle.UsuarioControlle;
import org.example.model.Usuario;

public class LoginView {

    private final UsuarioControlle usuarioControlle;

    public LoginView(UsuarioControlle usuarioControlle) {
        this.usuarioControlle = usuarioControlle;
    }

    public void trocarParaLogin(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 1px;");

        // 1. Adiciona a barra superior contínua (WindowBar)
        HBox topBar = WindowBar.criarBarraSuperior(stage, "Luna IA");
        root.setTop(topBar);

        // 2. Elementos do Formulário
        Label lblTitle = new Label("Entrar no Luna IA");
        lblTitle.getStyleClass().add("title");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("E-mail");

        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha");

        Button btnEntrar = new Button("Entrar");
        btnEntrar.getStyleClass().add("button-primary");

        Hyperlink linkCadastro = new Hyperlink("Não tem uma conta? Cadastre-se");
        linkCadastro.setStyle("-fx-text-fill: #a855f7; -fx-underline: true;");

        Label lblStatus = new Label();

        // Ação do Botão de Login
        btnEntrar.setOnAction(e -> {
            try {
                String email = txtEmail.getText();
                String senha = txtSenha.getText();

                if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
                    lblStatus.getStyleClass().removeAll("label-success");
                    lblStatus.getStyleClass().add("label-error");
                    lblStatus.setText("Preencha todos os campos!");
                    return;
                }

                // Autentica o usuário no MongoDB
                Usuario usuarioAutenticado = usuarioControlle.autenticar(email, senha);

                lblStatus.getStyleClass().removeAll("label-error");
                lblStatus.getStyleClass().add("label-success");
                lblStatus.setText("Login realizado com sucesso! Redirecionando...");

                System.out.println("-> Usuário autenticado: " + usuarioAutenticado.getNome());

                // Redireciona para a ChatView
                ChatView chatView = new ChatView(usuarioAutenticado);
                chatView.exibir(stage);

            } catch (Exception ex) {
                lblStatus.getStyleClass().removeAll("label-success");
                lblStatus.getStyleClass().add("label-error");
                lblStatus.setText("E-mail ou senha inválidos.");
            }
        });

        // Ação para navegar até a tela de Cadastro
        linkCadastro.setOnAction(e -> {
            RegisterView registerView = new RegisterView(usuarioControlle);
            registerView.trocarParaCadastro(stage);
        });

        // Centraliza o formulário no meio da tela
        VBox form = new VBox(lblTitle, txtEmail, txtSenha, btnEntrar, linkCadastro, lblStatus);
        form.getStyleClass().add("Form-container");
        form.setMaxWidth(350);

        VBox centerBox = new VBox(form);
        centerBox.setAlignment(Pos.CENTER);
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 450, 520);

        String css = getClass().getResource("/splash/register.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Login - Chat IA");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        stage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
    }
}