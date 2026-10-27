package org.example.View;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
import org.example.controlle.UsuarioControlle;
import org.example.model.Conversas;
import org.example.model.Mensagem;
import org.example.model.Usuario;
import org.example.repository.ConversaRepository;
import org.example.service.AIService;
import org.example.theme.ThemeManager;
import org.example.theme.ThemeManager.Tema;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatView {

    private final Usuario usuario;
    private final UsuarioControlle usuarioControlle;
    private final ConversaRepository conversaRepository = new ConversaRepository();

    private BorderPane root;
    private StackPane areaCentral;
    private BorderPane layoutChatAtual;
    private VBox chatContainer;
    private VBox listaConversasContainer;
    private VBox sidebar;
    private Label lblNomeUsuario;
    private TextField txtMensagemChat;
    private HBox inputBarChat;

    private boolean sidebarMinimizada = false;

    private final AIService aiService = new AIService();

    private final String nomeIA = "Luna IA";
    private String personalidadeIA = "Amigável";
    private String idioma = "Português";

    private String termoBuscaAtual = "";
    private boolean apenasFavoritos = false;
    private List<Conversas> listaConversasBanco = new ArrayList<>();
    private Conversas conversaAtual;

    private File imagemAnexadaBiblioteca = null;

    public ChatView(Usuario usuario, UsuarioControlle usuarioControlle) {
        this.usuario = usuario;
        this.usuarioControlle = usuarioControlle;
    }

    public void exibir(Stage stage) {
        root = new BorderPane();

        // Barra superior global integrada com o botão de alternar tema
        HBox topWindowBar = WindowBar.criarBarraSuperior(stage, "Luna IA", () -> aplicarTema());
        root.setTop(topWindowBar);

        sidebar = new VBox(10);
        sidebar.setPadding(new Insets(15));

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

        lblNomeUsuario = new Label(usuario != null && usuario.getNome() != null ? usuario.getNome() : "Usuário");
        lblNomeUsuario.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox perfilEsquerdaBox = new HBox(10, btnAlterarFoto, lblNomeUsuario);
        perfilEsquerdaBox.setAlignment(Pos.CENTER_LEFT);

        Button btnToggleSidebar = new Button("☰");
        btnToggleSidebar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0;");

        HBox topoSidebar = new HBox(perfilEsquerdaBox, btnToggleSidebar);
        topoSidebar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(perfilEsquerdaBox, Priority.ALWAYS);

        Button btnNovaConversa = new Button("+ Nova conversa");
        btnNovaConversa.setMaxWidth(Double.MAX_VALUE);
        btnNovaConversa.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px;");

        Button btnBiblioteca = criarBotaoMenu("📚 Biblioteca");
        Button btnImagens = criarBotaoMenu("🎨 Imagens");
        Button btnPersonalizar = criarBotaoMenu("⚙ Personalizar");
        Button btnConfiguracao = criarBotaoMenu("🛠 Configurações");
        Button btnEstatisticas = criarBotaoMenu("📊 Estatísticas");

        VBox menuOpcoes = new VBox(6, btnNovaConversa, btnBiblioteca, btnImagens, btnPersonalizar, btnConfiguracao, btnEstatisticas);

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Pesquisar...");
        txtBusca.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-prompt-text-fill: #94a3b8; -fx-background-radius: 6px; -fx-padding: 6px 10px; -fx-font-size: 12px;");
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
            this.termoBuscaAtual = newVal;
            aplicarFiltrosEAtualizar();
        });

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

        listaConversasContainer = new VBox(6);
        listaConversasContainer.setPadding(new Insets(5, 0, 5, 0));

        ScrollPane scrollConversas = new ScrollPane(listaConversasContainer);
        scrollConversas.setFitToWidth(true);
        scrollConversas.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollConversas, Priority.ALWAYS);

        carregarConversasDoBanco();

        VBox conteudoSidebar = new VBox(10, menuOpcoes, new Separator(), txtBusca, filtroBox, scrollConversas);
        VBox.setVgrow(conteudoSidebar, Priority.ALWAYS);

        sidebar.getChildren().addAll(topoSidebar, new Separator(), conteudoSidebar);
        root.setLeft(sidebar);

        btnToggleSidebar.setOnAction(e -> {
            sidebarMinimizada = !sidebarMinimizada;
            sidebar.setStyle(ThemeManager.getSidebarStyle(sidebarMinimizada));
            if (sidebarMinimizada) {
                lblNomeUsuario.setVisible(false);
                lblNomeUsuario.setManaged(false);
                conteudoSidebar.setVisible(false);
                conteudoSidebar.setManaged(false);
            } else {
                lblNomeUsuario.setVisible(true);
                lblNomeUsuario.setManaged(true);
                conteudoSidebar.setVisible(true);
                conteudoSidebar.setManaged(true);
            }
        });

        areaCentral = new StackPane();
        BorderPane layoutChat = criarLayoutChat();
        layoutChatAtual = layoutChat;
        areaCentral.getChildren().add(layoutChat);
        root.setCenter(areaCentral);

        btnNovaConversa.setOnAction(e -> {
            iniciarNovaConversaVazia();
            areaCentral.getChildren().setAll(layoutChat);
        });

        btnBiblioteca.setOnAction(e -> areaCentral.getChildren().setAll(criarTelaBiblioteca(stage)));
        btnImagens.setOnAction(e -> areaCentral.getChildren().setAll(criarTelaImagens()));
        btnPersonalizar.setOnAction(e -> areaCentral.getChildren().setAll(criarTelaPersonalizacao(layoutChat)));
        btnConfiguracao.setOnAction(e -> areaCentral.getChildren().setAll(criarTelaConfiguracoes(stage, layoutChat)));
        btnEstatisticas.setOnAction(e -> areaCentral.getChildren().setAll(criarTelaEstatisticas(layoutChat)));

        aplicarTema();

        Scene scene = new Scene(root, 1020, 660);
        stage.setTitle("Luna IA");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private void aplicarTema() {
        root.setStyle(ThemeManager.getRootStyle());
        sidebar.setStyle(ThemeManager.getSidebarStyle(sidebarMinimizada));
        lblNomeUsuario.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        if (txtMensagemChat != null) {
            txtMensagemChat.setStyle(ThemeManager.getTextFieldStyle());
        }
        if (inputBarChat != null) {
            inputBarChat.setStyle(ThemeManager.getTemaAtual() == Tema.CLARO ? "-fx-background-color: #e2e8f0;" : "-fx-background-color: #1e293b;");
        }
        aplicarFiltrosEAtualizar();
    }

    private VBox criarTelaConfiguracoes(Stage stage, BorderPane layoutChatVoltar) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(420);

        Label lblTitulo = new Label("🛠 Configurações");
        lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label lblNome = new Label("Nome de exibição");
        lblNome.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        TextField txtNome = new TextField(lblNomeUsuario.getText());
        txtNome.setStyle(ThemeManager.getTextFieldStyle());

        Label lblIdioma = new Label("Idioma das respostas da IA");
        lblIdioma.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        ComboBox<String> cbIdioma = new ComboBox<>();
        cbIdioma.getItems().addAll("Português", "English", "Español");
        cbIdioma.setValue(idioma);
        cbIdioma.setMaxWidth(Double.MAX_VALUE);

        Label lblStatus = new Label();
        lblStatus.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");

        Button btnSalvar = new Button("Salvar alterações");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10px; -fx-background-radius: 8px;");
        btnSalvar.setOnAction(e -> {
            String novoNome = txtNome.getText().trim();
            if (novoNome.isEmpty()) {
                lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                lblStatus.setText("O nome não pode ficar vazio.");
                return;
            }
            try {
                usuarioControlle.atualizarNome(usuario, novoNome);
            } catch (Exception ex) {
                lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                lblStatus.setText("Não foi possível salvar: " + ex.getMessage());
                return;
            }
            lblNomeUsuario.setText(novoNome);
            idioma = cbIdioma.getValue();

            lblStatus.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");
            lblStatus.setText("✓ Alterações salvas!");
        });

        Button btnVoltar = new Button("Voltar ao Chat");
        btnVoltar.setMaxWidth(Double.MAX_VALUE);
        btnVoltar.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 10px; -fx-background-radius: 8px;");
        btnVoltar.setOnAction(e -> areaCentral.getChildren().setAll(layoutChatVoltar));

        Button btnSair = new Button("🚪 Sair da conta");
        btnSair.setMaxWidth(Double.MAX_VALUE);
        btnSair.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10px; -fx-background-radius: 8px;");
        btnSair.setOnAction(e -> {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja realmente sair da conta?", ButtonType.YES, ButtonType.NO);
            confirmacao.setTitle("Sair da conta");
            confirmacao.setHeaderText(null);
            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.YES) {
                    conversaAtual = null;
                    new LoginView(usuarioControlle).trocarParaLogin(stage);
                }
            });
        });

        box.getChildren().addAll(lblTitulo, new Separator(),
                lblNome, txtNome,
                lblIdioma, cbIdioma,
                lblStatus, btnSalvar, btnVoltar,
                new Separator(), btnSair);
        return box;
    }

    private VBox criarTelaEstatisticas(BorderPane layoutChatVoltar) {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(450);

        Label lblTitulo = new Label("📊 Estatísticas da Conta");
        lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        int totalConversas = listaConversasBanco.size();
        long totalFavoritas = listaConversasBanco.stream().filter(Conversas::isFavorito).count();
        int totalMensagens = listaConversasBanco.stream().mapToInt(c -> c.getMensagens() != null ? c.getMensagens().size() : 0).sum();

        VBox card1 = criarCardEstatistica("Conversas Salvas", String.valueOf(totalConversas));
        VBox card2 = criarCardEstatistica("Conversas Favoritas", String.valueOf(totalFavoritas));
        VBox card3 = criarCardEstatistica("Total de Mensagens", String.valueOf(totalMensagens));
        VBox card4 = criarCardEstatistica("Personalidade Ativa", personalidadeIA);

        Button btnVoltar = new Button("Voltar ao Chat");
        btnVoltar.setMaxWidth(Double.MAX_VALUE);
        btnVoltar.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10px; -fx-background-radius: 8px;");
        btnVoltar.setOnAction(e -> areaCentral.getChildren().setAll(layoutChatVoltar));

        box.getChildren().addAll(lblTitulo, new Separator(), card1, card2, card3, card4, btnVoltar);
        return box;
    }

    private VBox criarCardEstatistica(String titulo, String valor) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(ThemeManager.getTemaAtual() == Tema.CLARO
                ? "-fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;"
                : "-fx-background-color: #1e293b; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        card.getChildren().addAll(lblTitulo, lblValor);
        return card;
    }

    private VBox criarTelaImagens() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(600);

        TextField txtPrompt = new TextField();
        txtPrompt.setPromptText("Ex: um gato astronauta, castelo no céu...");
        txtPrompt.setStyle(ThemeManager.getTextFieldStyle());
        txtPrompt.setMaxWidth(500);

        Button btnGerar = new Button("Gerar Imagem");
        btnGerar.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10px 24px; -fx-background-radius: 8px;");

        Label lblStatus = new Label();
        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(500);
        lblStatus.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");

        ImageView imgResultado = new ImageView();
        imgResultado.setFitWidth(400);
        imgResultado.setFitHeight(400);
        imgResultado.setPreserveRatio(true);
        imgResultado.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");

        StackPane paneImgWrapper = new StackPane(imgResultado);
        paneImgWrapper.setAlignment(Pos.CENTER);

        ScrollPane scrollImg = new ScrollPane(paneImgWrapper);
        scrollImg.setFitToWidth(true);
        scrollImg.setFitToHeight(true);
        scrollImg.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollImg.setMaxHeight(430);

        Runnable gerar = () -> {
            String ideia = txtPrompt.getText().trim();
            if (ideia.isEmpty()) return;

            imgResultado.setImage(null);
            btnGerar.setDisable(true);
            btnGerar.setText("Gerando...");
            lblStatus.setText("✨ A Luna está criando uma descrição detalhada...");

            Task<String> taskPrompt = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiService.melhorarPromptImagem(ideia);
                }
            };

            taskPrompt.setOnSucceeded(ev -> {
                String promptDetalhado = taskPrompt.getValue();
                lblStatus.setText("Prompt usado: " + promptDetalhado);
                carregarImagemGerada(promptDetalhado, imgResultado, btnGerar, lblStatus);
            });

            taskPrompt.setOnFailed(ev -> {
                lblStatus.setText("Não foi possível detalhar o prompt, usando sua ideia original.");
                carregarImagemGerada(ideia, imgResultado, btnGerar, lblStatus);
            });

            Thread t = new Thread(taskPrompt);
            t.setDaemon(true);
            t.start();
        };

        btnGerar.setOnAction(e -> gerar.run());
        txtPrompt.setOnAction(e -> gerar.run());

        box.getChildren().addAll(txtPrompt, btnGerar, lblStatus, scrollImg);
        return box;
    }

    private void carregarImagemGerada(String prompt, ImageView destino, Button btnGerar, Label lblStatus) {
        String promptCodificado = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "https://image.pollinations.ai/prompt/" + promptCodificado
                + "?width=512&height=512&nologo=true&seed=" + (int) (Math.random() * 100000);

        Image img = new Image(url, true);
        destino.setImage(img);

        img.progressProperty().addListener((obs, antigo, novo) -> {
            if (novo.doubleValue() >= 1.0) {
                btnGerar.setDisable(false);
                btnGerar.setText("Gerar Imagem");
            }
        });
        img.errorProperty().addListener((obs, antigo, erro) -> {
            if (erro) {
                lblStatus.setText("Erro ao carregar a imagem. Tente novamente.");
                btnGerar.setDisable(false);
                btnGerar.setText("Gerar Imagem");
            }
        });
    }

    private VBox criarTelaBiblioteca(Stage stage) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setMaxWidth(600);

        Label lblTitulo = new Label("📚 Biblioteca");
        lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        TextArea txtTexto = new TextArea();
        txtTexto.setPromptText("Escreva ou cole aqui o seu texto...");
        txtTexto.setPrefRowCount(7);
        txtTexto.setWrapText(true);
        txtTexto.setStyle(ThemeManager.getTextFieldStyle());
        txtTexto.setMaxWidth(500);

        TextField txtPergunta = new TextField();
        txtPergunta.setPromptText("O que fazer com o texto? (resumir, corrigir, traduzir, perguntar...)");
        txtPergunta.setStyle(ThemeManager.getTextFieldStyle());
        txtPergunta.setMaxWidth(500);

        Button btnAnexar = new Button("📎 Anexar Imagem (opcional)");
        btnAnexar.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8px 16px; -fx-background-radius: 6px;");

        Label lblNomeArquivo = new Label("Nenhuma imagem anexada.");
        lblNomeArquivo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        ImageView imgPreview = new ImageView();
        imgPreview.setFitWidth(120);
        imgPreview.setFitHeight(120);
        imgPreview.setPreserveRatio(true);

        btnAnexar.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecionar Imagem");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                imagemAnexadaBiblioteca = file;
                imgPreview.setImage(new Image(file.toURI().toString()));
                lblNomeArquivo.setText("Arquivo: " + file.getName());
            }
        });

        HBox anexoBox = new HBox(15, btnAnexar, lblNomeArquivo, imgPreview);
        anexoBox.setAlignment(Pos.CENTER);

        Button btnEnviarIA = new Button("Enviar à Luna IA");
        btnEnviarIA.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10px 20px; -fx-background-radius: 8px;");

        Label lblRespostaIA = new Label();
        lblRespostaIA.setWrapText(true);
        lblRespostaIA.setMaxWidth(480);
        lblRespostaIA.setStyle(ThemeManager.getBalaoIAGetStyle());

        ScrollPane scrollResposta = new ScrollPane(lblRespostaIA);
        scrollResposta.setFitToWidth(true);
        scrollResposta.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollResposta.setMaxHeight(160);

        btnEnviarIA.setOnAction(e -> {
            String texto = txtTexto.getText().trim();
            String pedido = txtPergunta.getText().trim();

            if (texto.isEmpty() && imagemAnexadaBiblioteca == null) {
                lblRespostaIA.setText("Escreva um texto ou anexe uma imagem primeiro.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            if (!texto.isEmpty()) {
                sb.append("Texto do usuário:\n").append(texto).append("\n\n");
            }
            if (imagemAnexadaBiblioteca != null) {
                sb.append("[O usuário anexou a imagem '").append(imagemAnexadaBiblioteca.getName()).append("']\n\n");
            }
            sb.append("Pedido: ").append(pedido.isEmpty() ? "Analise e comente o conteúdo acima." : pedido);

            List<Mensagem> historico = new ArrayList<>();
            historico.add(new Mensagem(sb.toString(), true));

            lblRespostaIA.setText("Analisando e pensando...");
            btnEnviarIA.setDisable(true);

            Task<String> taskAnalise = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiService.gerarResposta(historico, personalidadeIA, idioma);
                }
            };

            taskAnalise.setOnSucceeded(event -> {
                lblRespostaIA.setText(taskAnalise.getValue());
                btnEnviarIA.setDisable(false);
            });

            taskAnalise.setOnFailed(event -> {
                lblRespostaIA.setText("Erro ao processar a solicitação com a IA.");
                btnEnviarIA.setDisable(false);
            });

            Thread t = new Thread(taskAnalise);
            t.setDaemon(true);
            t.start();
        });

        box.getChildren().addAll(lblTitulo, txtTexto, txtPergunta, anexoBox, btnEnviarIA, scrollResposta);
        return box;
    }

    private BorderPane criarLayoutChat() {
        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollChat = new ScrollPane(chatContainer);
        scrollChat.setFitToWidth(true);
        scrollChat.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatContainer.heightProperty().addListener((obs, o, n) -> scrollChat.setVvalue(1.0));

        carregarMensagensNaTela();

        inputBarChat = new HBox(10);
        inputBarChat.setPadding(new Insets(15));
        inputBarChat.setAlignment(Pos.CENTER);

        txtMensagemChat = new TextField();
        txtMensagemChat.setPromptText("Digite sua mensagem...");
        HBox.setHgrow(txtMensagemChat, Priority.ALWAYS);

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8px; -fx-padding: 10px 18px;");

        btnEnviar.setOnAction(e -> {
            String texto = txtMensagemChat.getText().trim();
            if (texto.isEmpty()) return;

            if (conversaAtual == null) {
                String titulo = texto.length() > 25 ? texto.substring(0, 25) + "..." : texto;
                conversaAtual = new Conversas(usuario.getEmail(), titulo);
            }

            txtMensagemChat.clear();

            if (texto.toLowerCase().startsWith("/imagem") || texto.toLowerCase().startsWith("imagem:") || texto.toLowerCase().contains("gerar imagem")) {
                adicionarBalao(chatContainer, texto, true);
                conversaAtual.getMensagens().add(new Mensagem(texto, true));

                String aviso = "Para gerar imagens, por favor acesse a aba '🎨 Imagens' no menu lateral esquerdo!";
                adicionarBalao(chatContainer, aviso, false);
                conversaAtual.getMensagens().add(new Mensagem(aviso, false));

                conversaRepository.salvar(conversaAtual);
                carregarConversasDoBanco();
                return;
            }

            adicionarBalao(chatContainer, texto, true);
            conversaAtual.getMensagens().add(new Mensagem(texto, true));

            Label lblDigitando = new Label("Luna IA está pensando...");
            lblDigitando.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 5px;");
            chatContainer.getChildren().add(lblDigitando);

            final List<Mensagem> historico = new ArrayList<>(conversaAtual.getMensagens());
            final String personalidadeNaHora = personalidadeIA;
            final String idiomaNaHora = idioma;
            final Conversas conversaDaResposta = conversaAtual;

            Task<String> taskGroq = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiService.gerarResposta(historico, personalidadeNaHora, idiomaNaHora);
                }
            };

            taskGroq.setOnSucceeded(event -> {
                String respostaIA = taskGroq.getValue();
                conversaDaResposta.getMensagens().add(new Mensagem(respostaIA, false));
                conversaRepository.salvar(conversaDaResposta);

                if (conversaAtual == conversaDaResposta) {
                    chatContainer.getChildren().remove(lblDigitando);
                    adicionarBalao(chatContainer, respostaIA, false);
                }
                carregarConversasDoBanco();
            });

            taskGroq.setOnFailed(event -> {
                chatContainer.getChildren().remove(lblDigitando);
                adicionarBalao(chatContainer, "Erro de conexão com a Groq.", false);
            });

            Thread t = new Thread(taskGroq);
            t.setDaemon(true);
            t.start();
        });

        txtMensagemChat.setOnAction(e -> btnEnviar.fire());
        inputBarChat.getChildren().addAll(txtMensagemChat, btnEnviar);

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollChat);
        layout.setBottom(inputBarChat);
        return layout;
    }

    private String saudacao() {
        switch (idioma) {
            case "English":
                return "Hi! I'm " + nomeIA + ". How can I help you today?";
            case "Español":
                return "¡Hola! Soy " + nomeIA + ". ¿Cómo puedo ayudarte hoy?";
            default:
                return "Olá! Sou a " + nomeIA + ". Como posso te ajudar hoje?";
        }
    }

    private void carregarConversasDoBanco() {
        if (usuario != null && usuario.getEmail() != null) {
            listaConversasBanco = conversaRepository.buscarPorUsuario(usuario.getEmail());
        } else {
            listaConversasBanco = new ArrayList<>();
        }
        aplicarFiltrosEAtualizar();
    }

    private void iniciarNovaConversaVazia() {
        conversaAtual = null;
        chatContainer.getChildren().clear();
        adicionarBalao(chatContainer, saudacao(), false);
    }

    private void carregarMensagensNaTela() {
        chatContainer.getChildren().clear();
        if (conversaAtual == null || conversaAtual.getMensagens().isEmpty()) {
            adicionarBalao(chatContainer, saudacao(), false);
        } else {
            for (Mensagem m : conversaAtual.getMensagens()) {
                adicionarBalao(chatContainer, m.getTexto(), m.isUsuario());
            }
        }
    }

    private void aplicarFiltrosEAtualizar() {
        List<Conversas> listaFiltrada = listaConversasBanco.stream()
                .filter(item -> !apenasFavoritos || item.isFavorito())
                .filter(item -> termoBuscaAtual.isBlank() || item.getTitulo().toLowerCase().contains(termoBuscaAtual.toLowerCase()))
                .collect(Collectors.toList());

        atualizarListaConversas(listaFiltrada);
    }

    private void atualizarListaConversas(List<Conversas> conversas) {
        listaConversasContainer.getChildren().clear();

        if (conversas.isEmpty()) {
            Label lblVazio = new Label("Nenhuma conversa.");
            lblVazio.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-padding: 8px;");
            listaConversasContainer.getChildren().add(lblVazio);
            return;
        }

        for (Conversas c : conversas) {
            HBox itemBox = new HBox(6);
            itemBox.setAlignment(Pos.CENTER_LEFT);
            itemBox.setPadding(new Insets(8));
            itemBox.setStyle(ThemeManager.getTemaAtual() == Tema.CLARO
                    ? "-fx-background-color: #cbd5e1; -fx-background-radius: 6px; -fx-cursor: hand;"
                    : "-fx-background-color: #334155; -fx-background-radius: 6px; -fx-cursor: hand;");

            Label lblTitulo = new Label(c.getTitulo());
            lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 12px;");
            HBox.setHgrow(lblTitulo, Priority.ALWAYS);

            lblTitulo.setOnMouseClicked(e -> {
                conversaAtual = c;
                carregarMensagensNaTela();
            });
            itemBox.setOnMouseClicked(e -> {
                conversaAtual = c;
                carregarMensagensNaTela();
            });

            Button btnEditar = new Button("✏");
            btnEditar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
            btnEditar.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(c.getTitulo());
                dialog.setTitle("Editar Conversa");
                dialog.setHeaderText("Renomear título da conversa:");
                dialog.setContentText("Novo título:");

                dialog.showAndWait().ifPresent(novoTitulo -> {
                    if (!novoTitulo.isBlank()) {
                        c.setTitulo(novoTitulo);
                        conversaRepository.salvar(c);
                        carregarConversasDoBanco();
                    }
                });
            });

            Button btnDeletar = new Button("🗑");
            btnDeletar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
            btnDeletar.setOnAction(e -> {
                if (c.getId() != null) {
                    conversaRepository.deletar(c.getId());
                    if (conversaAtual != null && conversaAtual.getId() != null && conversaAtual.getId().equals(c.getId())) {
                        iniciarNovaConversaVazia();
                    }
                    carregarConversasDoBanco();
                }
            });

            Button btnFav = new Button(c.isFavorito() ? "★" : "☆");
            btnFav.setStyle("-fx-background-color: transparent; -fx-text-fill: " + (c.isFavorito() ? "#facc15" : "#94a3b8") + "; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0;");
            btnFav.setOnAction(e -> {
                c.setFavorito(!c.isFavorito());
                conversaRepository.salvar(c);
                carregarConversasDoBanco();
            });

            itemBox.getChildren().addAll(lblTitulo, btnEditar, btnDeletar, btnFav);
            listaConversasContainer.getChildren().add(itemBox);
        }
    }

    private VBox criarTelaPersonalizacao(BorderPane layoutChatVoltar) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(420);

        Label lblTitulo = new Label("Personalizar " + nomeIA);
        lblTitulo.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 20px; -fx-font-weight: bold;");

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
        opcoesBox.setStyle(ThemeManager.getTemaAtual() == Tema.CLARO ? "-fx-background-color: #e2e8f0; -fx-background-radius: 8px;" : "-fx-background-color: #1e293b; -fx-background-radius: 8px;");

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
        rb.setStyle("-fx-text-fill: " + ThemeManager.getTextColor() + "; -fx-font-size: 13px; -fx-cursor: hand;");
        return rb;
    }

    private Button criarBotaoMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(ThemeManager.getTemaAtual() == Tema.CLARO
                ? "-fx-background-color: #cbd5e1; -fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 12px;"
                : "-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
        return btn;
    }

    private void estilarBotaoFiltro(Button btn, boolean ativo) {
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        if (ativo) {
            btn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 5px;");
        } else {
            btn.setStyle(ThemeManager.getTemaAtual() == Tema.CLARO
                    ? "-fx-background-color: #cbd5e1; -fx-text-fill: #0f172a; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 5px;"
                    : "-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 5px;");
        }
    }

    private void adicionarBalao(VBox container, String texto, boolean isUsuario) {
        Label label = new Label(texto);
        label.setWrapText(true);
        label.setMaxWidth(480);

        VBox conteudoBalao = new VBox(5);
        conteudoBalao.getChildren().add(label);

        if (!isUsuario) {
            Button btnCopiar = new Button("📋 Copiar");
            btnCopiar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2px 0 0 0;");

            btnCopiar.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(texto);
                clipboard.setContent(content);

                btnCopiar.setText("✓ Copiado!");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> btnCopiar.setText("📋 Copiar"));
                pause.play();
            });

            conteudoBalao.getChildren().add(btnCopiar);
        }

        HBox wrapper = new HBox(conteudoBalao);
        wrapper.setPadding(new Insets(4));

        if (isUsuario) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            label.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-padding: 10px 16px; -fx-background-radius: 16px 16px 2px 16px; -fx-font-size: 14px;");
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            label.setStyle(ThemeManager.getBalaoIAGetStyle());
        }

        container.getChildren().add(wrapper);
    }
}