package org.example.View;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

                // 1. Autentica o usuário no MongoDB
                Usuario usuarioAutenticado = usuarioControlle.autenticar(email, senha);

                lblStatus.getStyleClass().removeAll("label-error");
                lblStatus.getStyleClass().add("label-success");
                lblStatus.setText("Login realizado com sucesso! Redirecionando...");

                System.out.println("-> Usuário autenticado: " + usuarioAutenticado.getNome());

                // 2. REDIRECIONA PARA A CHATVIEW
                ChatView chatView = new ChatView(usuarioAutenticado);
                chatView.exibir(stage);

            } catch (Exception ex) {
                lblStatus.getStyleClass().removeAll("label-success");
                lblStatus.getStyleClass().add("label-error");
                lblStatus.setText("E-mail ou senha inválidos.");
            }
        });

        linkCadastro.setOnAction(e -> {
            RegisterView registerView = new RegisterView(usuarioControlle);
            registerView.trocarParaCadastro(stage);
        });

        VBox form = new VBox(lblTitle, txtEmail, txtSenha, btnEntrar, linkCadastro, lblStatus);
        form.getStyleClass().add("Form-container");
        form.setMaxWidth(350);

        VBox root = new VBox(form);
        root.setAlignment(Pos.CENTER);

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