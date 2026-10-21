package org.example.View;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Usuario;
import org.example.service.AIService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatView {

    private final Usuario usuario;
    private StackPane areaCentral;
    private VBox chatContainer;
    private VBox listaConversasContainer;
    private VBox sidebar;
    private boolean sidebarMinimizada = false;

    // Instância do serviço para chamada à API da Groq
    private final AIService aiService = new AIService();

    // Configuração Fixa da IA e Personalidade Selecionada
    private final String nomeIA = "Luna IA";
    private String personalidadeIA = "Amigável";

    // Filtros de Pesquisa e Histórico
    private String termoBuscaAtual = "";
    private boolean apenasFavoritos = false;
    private final List<ConversaItem> historicoConversas = new ArrayList<>();

    public ChatView(Usuario usuario) {
        this.usuario = usuario;
        carregarConversasExemplo();
    }

    public void exibir(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 1px;");

        // 1. BARRA SUPERIOR GLOBAL DA JANELA (Com Maximizar, Minimizar e Fechar)
        HBox topWindowBar = WindowBar.criarBarraSuperior(stage, "Luna IA");
        root.setTop(topWindowBar);

        // 2. PAINEL LATERAL ESQUERDO (SIDEBAR)
        sidebar = new VBox(10);
        sidebar.setPadding(new Insets(15));
        sidebar.setStyle("-fx-background-color: #1e293b; -fx-min-width: 270px; -fx-max-width: 270px;");

        // Perfil do Usuário
        ImageView imgPerfil = new ImageView();
        imgPerfil.setFitWidth(36);
        imgPerfil.setFitHeight(36);
        Circle clip = new Circle(18, 18, 18);
        imgPerfil.setClip(clip);
        imgPerfil.setImage(new Image("https://via.placeholder.com/150"));

        Button btnAlterarFoto = new Button();
        btnAlterarFoto.setGraphic(imgPerfil);
        btnAlterarFoto.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        btnAlterarFoto.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Escolher foto de perfil");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                imgPerfil.setImage(new Image(selectedFile.toURI().toString()));
            }
        });

        Label lblNomeUsuario = new Label(usuario != null && usuario.getNome() != null ? usuario.getNome() : "Usuário");
        lblNomeUsuario.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;");

        HBox perfilEsquerdaBox = new HBox(10, btnAlterarFoto, lblNomeUsuario);
        perfilEsquerdaBox.setAlignment(Pos.CENTER_LEFT);

        // Ícone ☰ para minimizar e maximizar a sidebar
        Button btnToggleSidebar = new Button("☰");
        btnToggleSidebar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0;");

        HBox topoSidebar = new HBox(perfilEsquerdaBox, btnToggleSidebar);
        topoSidebar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(perfilEsquerdaBox, Priority.ALWAYS);

        // Botões de Navegação Principais
        Button btnNovaConversa = new Button("+ Nova conversa");
        btnNovaConversa.setMaxWidth(Double.MAX_VALUE);
        btnNovaConversa.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px;");

        Button btnBiblioteca = criarBotaoMenu("📚 Biblioteca");
        Button btnImagens = criarBotaoMenu("🎨 Imagens");
        Button btnPersonalizar = criarBotaoMenu("⚙ Personalizar");
        Button btnConfiguracao = criarBotaoMenu("🛠 Configurações");

        VBox menuOpcoes = new VBox(6, btnNovaConversa, btnBiblioteca, btnImagens, btnPersonalizar, btnConfiguracao);

        // Lupa de Pesquisa
        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Pesquisar...");
        txtBusca.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-prompt-text-fill: #94a3b8; -fx-background-radius: 6px; -fx-padding: 6px 10px; -fx-font-size: 12px;");
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            this.termoBuscaAtual = newVal;
            aplicarFiltrosEAtualizar();
        });

        // Filtros (Todas / Favoritas)
        Button btnFiltroTodas = new Button("Todas");
        Button btnFiltroFavoritas = new Button("★ Favoritas");

        estilarBotaoFiltro(btnFiltroTodas, !apenasFavoritos);
        estilarBotaoFiltro(btnFiltroFavoritas, apenasFavoritos);

        btnFiltroTodas.setOnAction(e -> {
            apenasFavoritos = false;
            estilarBotaoFiltro(btnFiltroTodas, true);
            estilarBotaoFiltro(btnFiltroFavoritas, false);
            aplicarFiltrosEAtualizar();
        });

        btnFiltroFavoritas.setOnAction(e -> {
            apenasFavoritos = true;
            estilarBotaoFiltro(btnFiltroTodas, false);
            estilarBotaoFiltro(btnFiltroFavoritas, true);
            aplicarFiltrosEAtualizar();
        });

        HBox filtroBox = new HBox(8, btnFiltroTodas, btnFiltroFavoritas);
        filtroBox.setAlignment(Pos.CENTER);

        // Lista de Conversas
        listaConversasContainer = new VBox(6);
        listaConversasContainer.setPadding(new Insets(5, 0, 5, 0));

        ScrollPane scrollConversas = new ScrollPane(listaConversasContainer);
        scrollConversas.setFitToWidth(true);
        scrollConversas.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollConversas, Priority.ALWAYS);

        aplicarFiltrosEAtualizar();

        VBox conteudoSidebar = new VBox(10, menuOpcoes, new Separator(), txtBusca, filtroBox, scrollConversas);
        VBox.setVgrow(conteudoSidebar, Priority.ALWAYS);

        sidebar.getChildren().addAll(topoSidebar, new Separator(), conteudoSidebar);
        root.setLeft(sidebar);

        // Alternar Minimizar / Expandir a Sidebar com ☰
        btnToggleSidebar.setOnAction(e -> {
            sidebarMinimizada = !sidebarMinimizada;
            if (sidebarMinimizada) {
                sidebar.setStyle("-fx-background-color: #1e293b; -fx-min-width: 65px; -fx-max-width: 65px;");
                lblNomeUsuario.setVisible(false);
                lblNomeUsuario.setManaged(false);
                conteudoSidebar.setVisible(false);
                conteudoSidebar.setManaged(false);
            } else {
                sidebar.setStyle("-fx-background-color: #1e293b; -fx-min-width: 270px; -fx-max-width: 270px;");
                lblNomeUsuario.setVisible(true);
                lblNomeUsuario.setManaged(true);
                conteudoSidebar.setVisible(true);
                conteudoSidebar.setManaged(true);
            }
        });

        // 3. ÁREA CENTRAL
        areaCentral = new StackPane();

        // Layout Principal do Chat
        BorderPane layoutChat = criarLayoutChat();

        areaCentral.getChildren().add(layoutChat);
        root.setCenter(areaCentral);

        // Ações dos Botões do Menu
        btnNovaConversa.setOnAction(e -> {
            chatContainer.getChildren().clear();
            adicionarBalao(chatContainer, "Nova conversa iniciada com a " + nomeIA + "!", false);
            areaCentral.getChildren().setAll(layoutChat);
        });

        btnBiblioteca.setOnAction(e -> {
            areaCentral.getChildren().setAll(criarTelaGenerica("Biblioteca", "Sua biblioteca de conversas e tópicos salvos."));
        });

        btnImagens.setOnAction(e -> {
            areaCentral.getChildren().setAll(criarTelaGenerica("Galeria de Imagens", "Área para gerar e consultar imagens da " + nomeIA + "."));
        });

        btnPersonalizar.setOnAction(e -> {
            areaCentral.getChildren().setAll(criarTelaPersonalizacao(layoutChat));
        });

        btnConfiguracao.setOnAction(e -> {
            areaCentral.getChildren().setAll(criarTelaGenerica("Configurações", "Gerenciamento de conta, tema escuro e armazenamento."));
        });

        Scene scene = new Scene(root, 1020, 660);
        stage.setTitle("Luna IA");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private BorderPane criarLayoutChat() {
        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollChat = new ScrollPane(chatContainer);
        scrollChat.setFitToWidth(true);
        scrollChat.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        adicionarBalao(chatContainer, "Olá! Sou a " + nomeIA + " (Modo " + personalidadeIA + "). Como posso te ajudar hoje?", false);

        HBox inputBar = new HBox(10);
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

                Label lblDigitando = new Label("Luna IA está pensando...");
                lblDigitando.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 5px;");
                chatContainer.getChildren().add(lblDigitando);

                Task<String> taskGroq = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return aiService.gerarrResposta(texto, personalidadeIA);
                    }
                };

                taskGroq.setOnSucceeded(event -> {
                    chatContainer.getChildren().remove(lblDigitando);
                    String respostaIA = taskGroq.getValue();
                    adicionarBalao(chatContainer, respostaIA, false);
                });

                taskGroq.setOnFailed(event -> {
                    chatContainer.getChildren().remove(lblDigitando);
                    adicionarBalao(chatContainer, "Erro de conexão com a Groq. Verifique a chave de API e tente novamente.", false);
                });

                new Thread(taskGroq).start();
            }
        });

        txtMensagem.setOnAction(e -> btnEnviar.fire());
        inputBar.getChildren().addAll(txtMensagem, btnEnviar);

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollChat);
        layout.setBottom(inputBar);
        return layout;
    }

    private VBox criarTelaPersonalizacao(BorderPane layoutChatVoltar) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(420);

        Label lblTitulo = new Label("Personalizar " + nomeIA);
        lblTitulo.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label lblSub = new Label("Selecione a personalidade para as respostas:");
        lblSub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        ToggleGroup groupPersonalidades = new ToggleGroup();

        RadioButton rbAmigavel = criarRadioPersonalidade("Amigável", groupPersonalidades);
        RadioButton rbEmpatica = criarRadioPersonalidade("Empática", groupPersonalidades);
        RadioButton rbProfissional = criarRadioPersonalidade("Profissional", groupPersonalidades);
        RadioButton rbFormal = criarRadioPersonalidade("Formal", groupPersonalidades);
        RadioButton rbTecnica = criarRadioPersonalidade("Técnica", groupPersonalidades);
        RadioButton rbDireto = criarRadioPersonalidade("Modo Direto", groupPersonalidades);
        RadioButton rbHumoristica = criarRadioPersonalidade("Humorística", groupPersonalidades);

        for (Toggle toggle : groupPersonalidades.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            if (rb.getText().equals(personalidadeIA)) {
                rb.setSelected(true);
                break;
            }
        }

        VBox opcoesBox = new VBox(10, rbAmigavel, rbEmpatica, rbProfissional, rbFormal, rbTecnica, rbDireto, rbHumoristica);
        opcoesBox.setAlignment(Pos.CENTER_LEFT);
        opcoesBox.setPadding(new Insets(10));
        opcoesBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px;");

        Button btnSalvar = new Button("Salvar e Aplicar");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10px; -fx-background-radius: 6px;");

        btnSalvar.setOnAction(e -> {
            RadioButton selecionado = (RadioButton) groupPersonalidades.getSelectedToggle();
            if (selecionado != null) {
                this.personalidadeIA = selecionado.getText();
            }

            chatContainer.getChildren().clear();
            adicionarBalao(chatContainer, "Personalidade atualizada! Modo ativo: " + personalidadeIA, false);
            areaCentral.getChildren().setAll(layoutChatVoltar);
        });

        box.getChildren().addAll(lblTitulo, lblSub, opcoesBox, btnSalvar);
        return box;
    }

    private RadioButton criarRadioPersonalidade(String texto, ToggleGroup group) {
        RadioButton rb = new RadioButton(texto);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-cursor: hand;");
        return rb;
    }

    private Button criarBotaoMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
        return btn;
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

    private void carregarConversasExemplo() {
        historicoConversas.add(new ConversaItem("Dúvidas de Spring Boot e MongoDB", true));
        historicoConversas.add(new ConversaItem("Análise de requisitos do Chat", false));
        historicoConversas.add(new ConversaItem("Estrutura da tela JavaFX", true));
        historicoConversas.add(new ConversaItem("Comandos Git e repositórios", false));
    }

    private void aplicarFiltrosEAtualizar() {
        List<ConversaItem> listaFiltrada = historicoConversas.stream()
                .filter(item -> !apenasFavoritos || item.favorito)
                .filter(item -> termoBuscaAtual.isBlank() || item.titulo.toLowerCase().contains(termoBuscaAtual.toLowerCase()))
                .collect(Collectors.toList());

        atualizarListaConversas(listaFiltrada);
    }

    private void atualizarListaConversas(List<ConversaItem> conversas) {
        listaConversasContainer.getChildren().clear();

        if (conversas.isEmpty()) {
            Label lblVazio = new Label("Nenhuma conversa.");
            lblVazio.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-padding: 8px;");
            listaConversasContainer.getChildren().add(lblVazio);
            return;
        }

        for (ConversaItem item : conversas) {
            HBox itemBox = new HBox(6);
            itemBox.setAlignment(Pos.CENTER_LEFT);
            itemBox.setPadding(new Insets(8));
            itemBox.setStyle("-fx-background-color: #334155; -fx-background-radius: 6px; -fx-cursor: hand;");

            Label lblTitulo = new Label(item.titulo);
            lblTitulo.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px;");
            HBox.setHgrow(lblTitulo, Priority.ALWAYS);

            // Botão de Favoritar
            Button btnFav = new Button(item.favorito ? "★" : "☆");
            btnFav.setStyle("-fx-background-color: transparent; -fx-text-fill: " + (item.favorito ? "#facc15" : "#94a3b8") + "; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0;");

            btnFav.setOnAction(e -> {
                item.favorito = !item.favorito;
                aplicarFiltrosEAtualizar();
            });

            itemBox.getChildren().addAll(lblTitulo, btnFav);
            listaConversasContainer.getChildren().add(itemBox);
        }
    }

    private void estilarBotaoFiltro(Button btn, boolean ativo) {
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        if (ativo) {
            btn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 5px;");
        } else {
            btn.setStyle("-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 5px;");
        }
    }

    private void adicionarBalao(VBox container, String texto, boolean isUsuario) {
        Label label = new Label(texto);
        label.setWrapText(true);
        label.setMaxWidth(480);

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

    private static class ConversaItem {
        String titulo;
        boolean favorito;

        ConversaItem(String titulo, boolean favorito) {
            this.titulo = titulo;
            this.favorito = favorito;
        }
    }
}