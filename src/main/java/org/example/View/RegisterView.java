package org.example.View;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import org.example.controlle.UsuarioControlle;
import org.example.model.Usuario;

public class RegisterView {

    private final UsuarioControlle usuarioControlle;

    public RegisterView(UsuarioControlle usuarioControlle) {
        this.usuarioControlle = usuarioControlle;
    }

    public void trocarParaCadastro(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 1px;");

        // 1. Adiciona a barra superior contínua (WindowBar)
        HBox topBar = WindowBar.criarBarraSuperior(stage, "Luna IA");
        root.setTop(topBar);

        // 2. Elementos do Formulário
        Label lblTitle = new Label("Criar Conta");
        lblTitle.getStyleClass().add("title");

        TextField txtNome = new TextField();
        txtNome.setPromptText("Nome completo");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("E-mail");

        PasswordField textSenha = new PasswordField();
        textSenha.setPromptText("Senha (mínimo 8 caracteres)");

        PasswordField txtConfirmacao = new PasswordField();
        txtConfirmacao.setPromptText("Confirme sua senha");

        Button btnCadastrar = new Button("Cadastrar");
        btnCadastrar.getStyleClass().add("button-primary");

        Hyperlink linkLogin = new Hyperlink("Já tem uma conta? Faça login");
        linkLogin.setStyle("-fx-text-fill: #a855f7; -fx-underline: true;");

        Label lblStatus = new Label();

        btnCadastrar.setOnAction(e -> {
            try {
                Usuario novoUsuario = new Usuario();
                novoUsuario.setNome(txtNome.getText());
                novoUsuario.setEmail(txtEmail.getText());
                novoUsuario.setSenha(textSenha.getText());

                usuarioControlle.cadastrar(novoUsuario, txtConfirmacao.getText());

                lblStatus.getStyleClass().removeAll("label-error");
                lblStatus.getStyleClass().add("label-success");
                lblStatus.setText("Usuário cadastrado com sucesso! Redirecionando...");

                txtNome.clear();
                txtEmail.clear();
                textSenha.clear();
                txtConfirmacao.clear();

                // Aguarda 1.5 segundo e redireciona automaticamente para o Login
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(event -> {
                    LoginView loginView = new LoginView(usuarioControlle);
                    loginView.trocarParaLogin(stage);
                });
                delay.play();

            } catch (Exception ex) {
                lblStatus.getStyleClass().removeAll("label-success");
                lblStatus.getStyleClass().add("label-error");
                lblStatus.setText(ex.getMessage());
            }
        });

        // Ação do link para abrir a LoginView diretamente
        linkLogin.setOnAction(e -> {
            LoginView loginView = new LoginView(usuarioControlle);
            loginView.trocarParaLogin(stage);
        });

        // Centraliza o formulário no meio da tela
        VBox form = new VBox(lblTitle, txtNome, txtEmail, textSenha, txtConfirmacao, btnCadastrar, linkLogin, lblStatus);
        form.getStyleClass().add("Form-container");
        form.setMaxWidth(350);

        VBox centerBox = new VBox(form);
        centerBox.setAlignment(Pos.CENTER);
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 450, 520);

        String css = getClass().getResource("/splash/register.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Cadastro - Chat IA");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        stage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
    }
}