package org.example.View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Usuario;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatView {
    private final Usuario usuario;
    private StackPane areaCentral;
    private VBox chatContainer;
    private VBox sidebar;
    private boolean sidebarMinimizada= false;
    //Config Fixa de Ia e personalidade selecionada
    private final String nomeIA= "Luna IA";
    private String personalidadeIa= "Amigável";

    // Filtro de Pesquisa e Historico
    private String termoBuscarAtual="";
    private boolean apenasFavoritos=false;
    private final List<ConversaItem>historicoConversas= new ArrayList<>();

    public ChatView(Usuario usuario) {
        this.usuario = usuario;
    }
    public void exibir(Stage stage){
        BorderPane root= new BorderPane();
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 1px;");

        //Barra superior global da janela
        HBox topWindowBar= WindowBar.criarBarraSuperior(stage,"Luna Ia");
        root.setTop(topWindowBar);

        ///Painel Lateral esquerdo (sidebar)
        sidebar= new VBox(10);
        sidebar.setPadding(new Insets(15));
        sidebar.setStyle("-fx-background-color: #1e293b; -fx-min-width: 270px; -fx-max-width: 270px;");

        //Perfil do usuario
        ImageView imgPerfil= new ImageView();
        imgPerfil.setFitWidth(36);
        imgPerfil.setFitHeight(36);
        Circle clip = new Circle(18,18,18);
        imgPerfil.setClip(clip);
        imgPerfil.setImage(new Image("https://via.placeholder.com/150"));

        Button btnAlterarFoto = new Button();
        btnAlterarFoto.setGraphic(imgPerfil);
        btnAlterarFoto.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        btnAlterarFoto.setOnAction(e->{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Escolher foto de perfil");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("imagens","*.png","-.jpg","*.jpeg")

            );
            File selectedFile= fileChooser.showOpenDialog(stage);
            if (selectedFile!= null){
                imgPerfil.setImage(new Image(selectedFile.toURL().toString()));
            }

        });
        Label lblNomeUsuario = new Label(usuario!= null && usuario.getNome()!=null? usuario.getNome():"Usuário");
        lblNomeUsuario.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;");

        HBox perfilEsquerdaBox= new HBox(10,btnAlterarFoto,lblNomeUsuario);
        perfilEsquerdaBox.setAlignment(Pos.BASELINE_LEFT);

        //icone para minimizar e maximizar a sidebar
        Button btnToggleSidebar = new Button("☰");
        btnToggleSidebar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0;");

        HBox topoSidebar = new HBox(perfilEsquerdaBox,btnToggleSidebar);



    }
}
