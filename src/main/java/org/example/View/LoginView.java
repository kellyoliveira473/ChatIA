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
import org.example.theme.ThemeManager;
import org.example.theme.ThemeManager.Tema;

public class LoginView {

    private final UsuarioControlle usuarioControlle;
    private BorderPane root;
    private Label lblTitle;
    private TextField txtEmail;
    private PasswordField txtSenha;

    public LoginView(UsuarioControlle usuarioControlle) {
        this.usuarioControlle = usuarioControlle;
    }

    public void trocarParaLogin(Stage stage) {
        root = new BorderPane();

        // 1. Adiciona a barra superior global (WindowBar) passando o callback de atualizar tema
        HBox topBar = WindowBar.criarBarraSuperior(stage, "Luna IA", () -> aplicarTema());
        root.setTop(topBar);

        // 2. Elementos do Formulário
        lblTitle = new Label("Entrar no Luna IA");
        lblTitle.getStyleClass().add("title");

        txtEmail = new TextField();
        txtEmail.setPromptText("E-mail");

        txtSenha = new PasswordField();
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
                ChatView chatView = new ChatView(usuarioAutenticado, usuarioControlle);
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
        VBox form = new VBox(12, lblTitle, txtEmail, txtSenha, btnEntrar, linkCadastro, lblStatus);
        form.getStyleClass().add("Form-container");
        form.setMaxWidth(350);
        form.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(form);
        centerBox.setAlignment(Pos.CENTER);
        root.setCenter(centerBox);

        // Aplica o tema inicial configurado
        aplicarTema();

        Scene scene = new Scene(root, 450, 520);

        String css = getClass().getResource("/splash/register.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Login - Chat IA");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();

        stage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
    }

    private void aplicarTema() {
        if (root != null) {
            root.setStyle(ThemeManager.getRootStyle());
        }
        if (lblTitle != null) {
            lblTitle.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        }
    }
}