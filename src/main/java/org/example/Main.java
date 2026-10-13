package org.example;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.net.URL;

import org.example.controlle.UsuarioControlle;
import org.example.repository.UsuarioRepositoryMongo;
import org.example.service.UsuarioService;
import org.example.View.LoginView;

public class Main extends Application {

    private static boolean transitionDone = false;

    @Override
    public void start(Stage stage) {
        // Mantém o processo JavaFX ativo durante a troca de Scenes
        Platform.setImplicitExit(false);

        stage.initStyle(StageStyle.UNDECORATED);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        JavaBridge bridge = new JavaBridge(stage);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaConnector", bridge);
                    System.out.println("-> JavaBridge conectado com sucesso ao JavaScript!");
                } catch (Exception e) {
                    System.err.println("❌ Erro ao injetar JavaBridge: " + e.getMessage());
                }
            }
        });

        URL splashUrl = getClass().getResource("/splash/splash.html");
        if (splashUrl == null) {
            System.err.println("❌ ERRO CRÍTICO: Arquivo /splash/splash.html não encontrado!");
            return;
        }

        System.out.println("-> Carregando Splash HTML de: " + splashUrl.toExternalForm());
        engine.load(splashUrl.toExternalForm());

        Scene splashScene = new Scene(webView, 500, 350);
        stage.setScene(splashScene);
        stage.centerOnScreen();
        stage.show();

        // Temporizador de segurança caso a animação do JS não chame o JavaBridge
        PauseTransition fallbackTimer = new PauseTransition(Duration.seconds(6));
        fallbackTimer.setOnFinished(e -> {
            if (!transitionDone) {
                System.out.println("-> Temporizador acionado. Transicionando para LoginView...");
                bridge.onAnimationEnd();
            }
        });
        fallbackTimer.play();
    }

    public static class JavaBridge {
        private final Stage stage;

        public JavaBridge(Stage stage) {
            this.stage = stage;
        }

        public void onAnimationEnd() {
            if (transitionDone) {
                return;
            }
            transitionDone = true;

            Platform.runLater(() -> {
                try {
                    System.out.println("-> Transicionando da Splash HTML para LoginView...");

                    UsuarioRepositoryMongo repo = new UsuarioRepositoryMongo();
                    UsuarioService service = new UsuarioService(repo);
                    UsuarioControlle controller = new UsuarioControlle(service);

                    // Abre a tela de Login
                    LoginView loginView = new LoginView(controller);
                    loginView.trocarParaLogin(stage);

                    System.out.println("-> LoginView exibida na mesma janela com sucesso!");

                } catch (Exception e) {
                    System.err.println("❌ Erro ao abrir a tela de login:");
                    e.printStackTrace();
                    transitionDone = false;
                }
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}