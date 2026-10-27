package org.example.View;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.controlle.UsuarioControlle;
import org.example.model.Conversas;
import org.example.model.Mensagem;
import org.example.model.Usuario;
import org.example.repository.ConversaRepository;
import org.example.service.AIService;
import org.example.service.DocumentExtractor;
import org.example.theme.ThemeManager;
import org.example.theme.ThemeManager.Tema;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ChatView {

    private final Usuario usuario;
    private final UsuarioControlle usuarioControlle;
    private final ConversaRepository conversaRepository = new ConversaRepository();

    private Stage stagePrincipal;
    private HBox topWindowBar;          // criada uma vez; a WindowBar acompanha o tema sozinha
    private BorderPane rootAtual;
    private StackPane areaCentral;
    private BorderPane layoutChatAtual;
    private VBox chatContainer;
    private VBox listaConversasContainer;
    private VBox sidebar;
    private Label lblNomeUsuario;
    private TextField txtMensagem;
    private boolean sidebarMinimizada = false;

    // Qual tela está aberta (chat, configurações...). Usado para voltar a ela depois de trocar o tema.
    private Supplier<Node> telaAtual;
    private Consumer<Tema> ouvinteTema;

    private final AIService aiService = new AIService();

    private final String nomeIA = "Luna IA";
    private String personalidadeIA = "Amigável";
    private String idioma = "Português";
    private String nomeExibicao;
    private File fotoPerfil = null;

    private String termoBuscaAtual = "";
    private boolean apenasFavoritos = false;
    private List<Conversas> listaConversasBanco = new ArrayList<>();
    private Conversas conversaAtual;

    private File imagemAnexadaBiblioteca = null;
    private File documentoAnexado = null;
    private final Label lblNomeDocumento = new Label();

    public ChatView(Usuario usuario, UsuarioControlle usuarioControlle) {
        this.usuario = usuario;
        this.usuarioControlle = usuarioControlle;
        this.nomeExibicao = usuario != null && usuario.getNome() != null ? usuario.getNome() : "Usuário";
    }

    // ------------------------------------------------------------------
    //  Janela e tema
    // ------------------------------------------------------------------

    public void exibir(Stage stage) {
        this.stagePrincipal = stage;

        if (topWindowBar == null) {
            topWindowBar = WindowBar.criarBarraSuperior(stage, "Luna IA", null);
        }

        telaAtual = () -> layoutChatAtual;
        rootAtual = construirRoot();
        carregarConversasDoBanco();

        Scene scene = new Scene(rootAtual, 1020, 660);
        scene.setFill(Color.web(ThemeManager.getPaleta().fundo()));
        stage.setTitle("Luna IA");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        registrarOuvinteTema(stage);
    }

    private BorderPane construirRoot() {
        BorderPane root = new BorderPane();
        root.setStyle(ThemeManager.getRootStyle() + " " + ThemeManager.getControlesStyle());
        root.setTop(topWindowBar);
        root.setLeft(construirSidebar());

        areaCentral = new StackPane();
        layoutChatAtual = criarLayoutChat();
        areaCentral.getChildren().add(layoutChatAtual);
        root.setCenter(areaCentral);
        return root;
    }

    private void registrarOuvinteTema(Stage stage) {
        removerOuvinteTema();
        ouvinteTema = tema -> Platform.runLater(() -> reconstruirParaTema(stage));
        ThemeManager.aoMudarTema(ouvinteTema);
    }

    private void removerOuvinteTema() {
        if (ouvinteTema != null) {
            ThemeManager.removerOuvinte(ouvinteTema);
            ouvinteTema = null;
        }
    }

    /** Refaz a interface com as cores do novo tema, mantendo conversa, tela aberta e texto digitado. */
    private void reconstruirParaTema(Stage stage) {
        Scene scene = stage.getScene();
        if (scene == null || scene.getRoot() != rootAtual) {
            // Outra tela assumiu a janela (ex.: logout): não mexe nela.
            removerOuvinteTema();
            return;
        }

        String rascunho = txtMensagem != null ? txtMensagem.getText() : "";

        rootAtual.setTop(null); // solta a barra de título para reaproveitá-la no novo layout
        rootAtual = construirRoot();
        scene.setFill(Color.web(ThemeManager.getPaleta().fundo()));
        scene.setRoot(rootAtual);

        txtMensagem.setText(rascunho);
        txtMensagem.positionCaret(rascunho.length());

        if (telaAtual != null) {
            areaCentral.getChildren().setAll(telaAtual.get());
        }
    }

    private void mostrarTela(Supplier<Node> fabrica) {
        telaAtual = fabrica;
        areaCentral.getChildren().setAll(fabrica.get());
    }

    private void aplicarTemaDialogo(Dialog<?> dialogo) {
        if (stagePrincipal != null) {
            dialogo.initOwner(stagePrincipal);
        }
        dialogo.getDialogPane().setStyle(ThemeManager.getDialogStyle());
    }

    // ------------------------------------------------------------------
    //  Atalhos de estilo
    // ------------------------------------------------------------------

    private String estiloTexto(int tamanho, boolean negrito, boolean secundario) {
        return "-fx-text-fill: " + (secundario ? ThemeManager.getSecondaryTextColor() : ThemeManager.getTextColor())
                + "; -fx-font-size: " + tamanho + "px;"
                + (negrito ? " -fx-font-weight: bold;" : "");
    }

    private Button botaoPrimario(String texto) {
        Button botao = new Button(texto);
        ThemeManager.instalarHover(botao,
                () -> ThemeManager.getBotaoEnviarStyle(false),
                () -> ThemeManager.getBotaoEnviarStyle(true));
        return botao;
    }

    private Button botaoSecundario(String texto) {
        Button botao = new Button(texto);
        ThemeManager.instalarHover(botao,
                () -> ThemeManager.getBotaoSecundarioStyle(false),
                () -> ThemeManager.getBotaoSecundarioStyle(true));
        return botao;
    }

    private Button botaoPerigo(String texto) {
        Button botao = new Button(texto);
        ThemeManager.instalarHover(botao,
                () -> ThemeManager.getBotaoPerigoStyle(false),
                () -> ThemeManager.getBotaoPerigoStyle(true));
        return botao;
    }

    private Button criarBotaoMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        ThemeManager.instalarHover(btn,
                () -> ThemeManager.getBotaoSidebarStyle(false, false),
                () -> ThemeManager.getBotaoSidebarStyle(false, true));
        return btn;
    }

    private void estilarBotaoFiltro(Button btn, boolean ativo) {
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setStyle(ThemeManager.getBotaoFiltroStyle(ativo));
    }

    // ------------------------------------------------------------------
    //  Barra lateral
    // ------------------------------------------------------------------

    private StackPane criarAvatar() {
        String inicial = nomeExibicao.isBlank() ? "?" : nomeExibicao.substring(0, 1).toUpperCase();

        Circle fundoAvatar = new Circle(18, Color.web(ThemeManager.getPaleta().destaque()));
        Label lblInicial = new Label(inicial);
        lblInicial.setStyle("-fx-text-fill: " + ThemeManager.getPaleta().textoSobreDestaque()
                + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        ImageView imgPerfil = new ImageView();
        imgPerfil.setFitWidth(36);
        imgPerfil.setFitHeight(36);
        imgPerfil.setClip(new Circle(18, 18, 18));
        if (fotoPerfil != null) {
            imgPerfil.setImage(new Image(fotoPerfil.toURI().toString()));
        }

        StackPane avatar = new StackPane(fundoAvatar, lblInicial, imgPerfil);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setStyle("-fx-cursor: hand;");
        avatar.setOnMouseClicked(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Escolher foto de perfil");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(stagePrincipal);
            if (selectedFile != null) {
                fotoPerfil = selectedFile;
                imgPerfil.setImage(new Image(selectedFile.toURI().toString()));
            }
        });
        return avatar;
    }

    private VBox construirSidebar() {
        sidebar = new VBox(10);

        lblNomeUsuario = new Label(nomeExibicao);
        lblNomeUsuario.setStyle(estiloTexto(14, true, false));

        HBox perfilBox = new HBox(10, criarAvatar(), lblNomeUsuario);
        perfilBox.setAlignment(Pos.CENTER_LEFT);

        Button btnToggleSidebar = new Button("☰");
        btnToggleSidebar.setFocusTraversable(false);
        ThemeManager.instalarHover(btnToggleSidebar,
                () -> ThemeManager.getBotaoJanelaStyle(false, false),
                () -> ThemeManager.getBotaoJanelaStyle(true, false));

        HBox topoSidebar = new HBox(perfilBox, btnToggleSidebar);
        topoSidebar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(perfilBox, Priority.ALWAYS);

        Button btnNovaConversa = botaoPrimario("+ Nova conversa");
        btnNovaConversa.setMaxWidth(Double.MAX_VALUE);
        btnNovaConversa.setOnAction(e -> {
            iniciarNovaConversaVazia();
            mostrarTela(() -> layoutChatAtual);
            aplicarFiltrosEAtualizar();
        });

        Button btnBiblioteca = criarBotaoMenu("📚 Biblioteca");
        Button btnImagens = criarBotaoMenu("🎨 Imagens");
        Button btnPersonalizar = criarBotaoMenu("⚙ Personalizar");
        Button btnConfiguracao = criarBotaoMenu("🛠 Configurações");
        Button btnEstatisticas = criarBotaoMenu("📊 Estatísticas");

        btnBiblioteca.setOnAction(e -> mostrarTela(this::criarTelaBiblioteca));
        btnImagens.setOnAction(e -> mostrarTela(this::criarTelaImagens));
        btnPersonalizar.setOnAction(e -> mostrarTela(this::criarTelaPersonalizacao));
        btnConfiguracao.setOnAction(e -> mostrarTela(this::criarTelaConfiguracoes));
        btnEstatisticas.setOnAction(e -> mostrarTela(this::criarTelaEstatisticas));

        VBox menuOpcoes = new VBox(6, btnNovaConversa, btnBiblioteca, btnImagens, btnPersonalizar, btnConfiguracao, btnEstatisticas);

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Pesquisar...");
        ThemeManager.instalarCampoTexto(txtBusca);
        txtBusca.setText(termoBuscaAtual);
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
        scrollConversas.setStyle(ThemeManager.getScrollPaneStyle());
        VBox.setVgrow(scrollConversas, Priority.ALWAYS);

        VBox conteudoSidebar = new VBox(10, menuOpcoes, new Separator(), txtBusca, filtroBox, scrollConversas);
        VBox.setVgrow(conteudoSidebar, Priority.ALWAYS);

        sidebar.getChildren().addAll(topoSidebar, new Separator(), conteudoSidebar);

        Runnable aplicarEstadoSidebar = () -> {
            sidebar.setStyle(ThemeManager.getSidebarStyle(sidebarMinimizada));
            perfilBox.setVisible(!sidebarMinimizada);
            perfilBox.setManaged(!sidebarMinimizada);
            conteudoSidebar.setVisible(!sidebarMinimizada);
            conteudoSidebar.setManaged(!sidebarMinimizada);
        };
        aplicarEstadoSidebar.run();

        btnToggleSidebar.setOnAction(e -> {
            sidebarMinimizada = !sidebarMinimizada;
            aplicarEstadoSidebar.run();
        });

        aplicarFiltrosEAtualizar();
        return sidebar;
    }

    // ------------------------------------------------------------------
    //  Telas
    // ------------------------------------------------------------------

    private VBox criarTelaConfiguracoes() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(420);

        Label lblTitulo = new Label("🛠 Configurações");
        lblTitulo.setStyle(ThemeManager.getTituloGrandeStyle());

        Label lblNome = new Label("Nome de exibição");
        lblNome.setStyle(ThemeManager.getSubtituloStyle());
        TextField txtNome = new TextField(nomeExibicao);
        ThemeManager.instalarCampoTexto(txtNome);

        Label lblIdioma = new Label("Idioma das respostas da IA");
        lblIdioma.setStyle(ThemeManager.getSubtituloStyle());
        ComboBox<String> cbIdioma = new ComboBox<>();
        cbIdioma.getItems().addAll("Português", "English", "Español");
        cbIdioma.setValue(idioma);
        cbIdioma.setMaxWidth(Double.MAX_VALUE);

        Label lblAparencia = new Label("Aparência");
        lblAparencia.setStyle(ThemeManager.getSubtituloStyle());
        boolean escuro = ThemeManager.getTemaAtual() == Tema.ESCURO;
        Button btnTema = botaoSecundario(escuro ? "☀️ Mudar para tema claro" : "🌙 Mudar para tema escuro");
        btnTema.setMaxWidth(Double.MAX_VALUE);
        btnTema.setOnAction(e -> ThemeManager.alternarTema());

        Label lblStatus = new Label();
        lblStatus.setStyle(ThemeManager.getStatusStyle(false));

        Button btnSalvar = botaoPrimario("Salvar alterações");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> {
            String novoNome = txtNome.getText().trim();
            if (novoNome.isEmpty()) {
                lblStatus.setStyle(ThemeManager.getStatusStyle(true));
                lblStatus.setText("O nome não pode ficar vazio.");
                return;
            }
            try {
                usuarioControlle.atualizarNome(usuario, novoNome);
            } catch (Exception ex) {
                lblStatus.setStyle(ThemeManager.getStatusStyle(true));
                lblStatus.setText("Não foi possível salvar: " + ex.getMessage());
                return;
            }
            nomeExibicao = novoNome;
            lblNomeUsuario.setText(novoNome);
            idioma = cbIdioma.getValue();

            lblStatus.setStyle(ThemeManager.getStatusStyle(false));
            lblStatus.setText("✓ Alterações salvas!");
        });

        Button btnVoltar = botaoSecundario("Voltar ao Chat");
        btnVoltar.setMaxWidth(Double.MAX_VALUE);
        btnVoltar.setOnAction(e -> mostrarTela(() -> layoutChatAtual));

        Button btnSair = botaoPerigo("🚪 Sair da conta");
        btnSair.setMaxWidth(Double.MAX_VALUE);
        btnSair.setOnAction(e -> {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja realmente sair da conta?", ButtonType.YES, ButtonType.NO);
            confirmacao.setTitle("Sair da conta");
            confirmacao.setHeaderText(null);
            aplicarTemaDialogo(confirmacao);
            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.YES) {
                    conversaAtual = null;
                    removerOuvinteTema();
                    new LoginView(usuarioControlle).trocarParaLogin(stagePrincipal);
                }
            });
        });

        box.getChildren().addAll(lblTitulo, new Separator(),
                lblNome, txtNome,
                lblIdioma, cbIdioma,
                lblAparencia, btnTema,
                lblStatus, btnSalvar, btnVoltar,
                new Separator(), btnSair);
        return box;
    }

    private VBox criarTelaEstatisticas() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(450);

        Label lblTitulo = new Label("📊 Estatísticas da Conta");
        lblTitulo.setStyle(ThemeManager.getTituloGrandeStyle());

        int totalConversas = listaConversasBanco.size();
        long totalFavoritas = listaConversasBanco.stream().filter(Conversas::isFavorito).count();
        int totalMensagens = listaConversasBanco.stream().mapToInt(c -> c.getMensagens() != null ? c.getMensagens().size() : 0).sum();

        VBox card1 = criarCardEstatistica("Conversas Salvas", String.valueOf(totalConversas));
        VBox card2 = criarCardEstatistica("Conversas Favoritas", String.valueOf(totalFavoritas));
        VBox card3 = criarCardEstatistica("Total de Mensagens", String.valueOf(totalMensagens));
        VBox card4 = criarCardEstatistica("Personalidade Ativa", personalidadeIA);

        Button btnVoltar = botaoPrimario("Voltar ao Chat");
        btnVoltar.setMaxWidth(Double.MAX_VALUE);
        btnVoltar.setOnAction(e -> mostrarTela(() -> layoutChatAtual));

        box.getChildren().addAll(lblTitulo, new Separator(), card1, card2, card3, card4, btnVoltar);
        return box;
    }

    private VBox criarCardEstatistica(String titulo, String valor) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(ThemeManager.getCardStyle());

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle(ThemeManager.getSubtituloStyle());

        Label lblValor = new Label(valor);
        lblValor.setStyle(estiloTexto(18, true, false));

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
        ThemeManager.instalarCampoTexto(txtPrompt);
        txtPrompt.setMaxWidth(500);

        Button btnGerar = botaoPrimario("Gerar Imagem");

        Label lblStatus = new Label();
        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(500);
        lblStatus.setStyle(ThemeManager.getSubtituloStyle() + " -fx-font-style: italic;");

        ImageView imgResultado = new ImageView();
        imgResultado.setFitWidth(400);
        imgResultado.setFitHeight(400);
        imgResultado.setPreserveRatio(true);
        imgResultado.setStyle("-fx-effect: dropshadow(gaussian, " + ThemeManager.getPaleta().sombra() + ", 12, 0, 0, 4);");

        StackPane paneImgWrapper = new StackPane(imgResultado);
        paneImgWrapper.setAlignment(Pos.CENTER);

        ScrollPane scrollImg = new ScrollPane(paneImgWrapper);
        scrollImg.setFitToWidth(true);
        scrollImg.setFitToHeight(true);
        scrollImg.setStyle(ThemeManager.getScrollPaneStyle());
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

    private VBox criarTelaBiblioteca() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setMaxWidth(600);

        Label lblTitulo = new Label("📚 Biblioteca");
        lblTitulo.setStyle(ThemeManager.getTituloGrandeStyle());

        TextArea txtTexto = new TextArea();
        txtTexto.setPromptText("Escreva ou cole aqui o seu texto...");
        txtTexto.setPrefRowCount(7);
        txtTexto.setWrapText(true);
        ThemeManager.instalarCampoTexto(txtTexto);
        txtTexto.setMaxWidth(500);

        TextField txtPergunta = new TextField();
        txtPergunta.setPromptText("O que fazer com o texto? (resumir, corrigir, traduzir, perguntar...)");
        ThemeManager.instalarCampoTexto(txtPergunta);
        txtPergunta.setMaxWidth(500);

        Button btnAnexar = botaoSecundario("📎 Anexar Imagem (opcional)");

        Label lblNomeArquivo = new Label("Nenhuma imagem anexada.");
        lblNomeArquivo.setStyle(ThemeManager.getSubtituloStyle());

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
            File file = fileChooser.showOpenDialog(stagePrincipal);
            if (file != null) {
                imagemAnexadaBiblioteca = file;
                imgPreview.setImage(new Image(file.toURI().toString()));
                lblNomeArquivo.setText("Arquivo: " + file.getName());
            }
        });

        HBox anexoBox = new HBox(15, btnAnexar, lblNomeArquivo, imgPreview);
        anexoBox.setAlignment(Pos.CENTER);

        Button btnEnviarIA = botaoPrimario("Enviar à Luna IA");

        Label lblRespostaIA = new Label();
        lblRespostaIA.setWrapText(true);
        lblRespostaIA.setMaxWidth(480);
        lblRespostaIA.setPadding(new Insets(15));
        lblRespostaIA.setStyle(ThemeManager.getCardStyle() + " " + estiloTexto(13, false, false));

        ScrollPane scrollResposta = new ScrollPane(lblRespostaIA);
        scrollResposta.setFitToWidth(true);
        scrollResposta.setStyle(ThemeManager.getScrollPaneStyle());
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

    private VBox criarTelaPersonalizacao() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setMaxWidth(420);

        Label lblTitulo = new Label("Personalizar " + nomeIA);
        lblTitulo.setStyle(ThemeManager.getTituloGrandeStyle());

        Label lblSub = new Label("Selecione a personalidade para as respostas:");
        lblSub.setStyle(estiloTexto(13, false, true));

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
        opcoesBox.setPadding(new Insets(14));
        opcoesBox.setStyle(ThemeManager.getCardStyle());

        Button btnSalvar = botaoPrimario("Salvar e Aplicar");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);

        btnSalvar.setOnAction(e -> {
            RadioButton selecionado = (RadioButton) groupPersonalidades.getSelectedToggle();
            if (selecionado != null) {
                this.personalidadeIA = selecionado.getText();
            }

            chatContainer.getChildren().clear();
            adicionarBalao(chatContainer, "Personalidade atualizada! Modo ativo: " + personalidadeIA, false);
            mostrarTela(() -> layoutChatAtual);
        });

        box.getChildren().addAll(lblTitulo, lblSub, opcoesBox, btnSalvar);
        return box;
    }

    private RadioButton criarRadioPersonalidade(String texto, ToggleGroup group) {
        RadioButton rb = new RadioButton(texto);
        rb.setToggleGroup(group);
        rb.setStyle(estiloTexto(13, false, false) + " -fx-cursor: hand;");
        return rb;
    }

    // ------------------------------------------------------------------
    //  Chat
    // ------------------------------------------------------------------

    private BorderPane criarLayoutChat() {
        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollChat = new ScrollPane(chatContainer);
        scrollChat.setFitToWidth(true);
        scrollChat.setStyle(ThemeManager.getScrollPaneStyle());
        chatContainer.heightProperty().addListener((obs, o, n) -> scrollChat.setVvalue(1.0));

        carregarMensagensNaTela();

        // Botão para anexar documentos (PDF, Word, TXT) na barra do chat
        Button btnAnexarDoc = botaoSecundario("📎 Doc");
        btnAnexarDoc.setTooltip(new Tooltip("Anexar documento para R.A.G. (PDF, Word, TXT)"));

        lblNomeDocumento.setStyle("-fx-text-fill: " + ThemeManager.getCorSucesso() + "; -fx-font-size: 11px;");

        btnAnexarDoc.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecionar Documento para consulta da IA");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.docx", "*.txt")
            );
            File arquivoSelecionado = fileChooser.showOpenDialog(stagePrincipal);
            if (arquivoSelecionado != null) {
                documentoAnexado = arquivoSelecionado;
                lblNomeDocumento.setText("📄 " + arquivoSelecionado.getName());
            }
        });

        txtMensagem = new TextField();
        txtMensagem.setPromptText("Digite sua mensagem (com ou sem documento)...");
        ThemeManager.instalarCampoTexto(txtMensagem);
        HBox.setHgrow(txtMensagem, Priority.ALWAYS);

        Button btnEnviar = botaoPrimario("Enviar");

        btnEnviar.setOnAction(e -> {
            String texto = txtMensagem.getText().trim();
            if (texto.isEmpty() && documentoAnexado == null) return;

            if (conversaAtual == null) {
                String titulo = texto.length() > 25 ? texto.substring(0, 25) + "..." : (documentoAnexado != null ? documentoAnexado.getName() : "Conversa");
                conversaAtual = new Conversas(usuario.getEmail(), titulo);
            }

            txtMensagem.clear();

            String textoDocumento = null;
            if (documentoAnexado != null) {
                textoDocumento = DocumentExtractor.extrairTexto(documentoAnexado);
                String avisoDoc = "[Documento anexado: " + documentoAnexado.getName() + "]";
                adicionarBalao(chatContainer, avisoDoc, true);
                conversaAtual.getMensagens().add(new Mensagem(avisoDoc, true));

                documentoAnexado = null;
                lblNomeDocumento.setText("");
            }

            if (!texto.isEmpty()) {
                adicionarBalao(chatContainer, texto, true);
                conversaAtual.getMensagens().add(new Mensagem(texto, true));
            }

            if (texto.toLowerCase().startsWith("/imagem") || texto.toLowerCase().startsWith("imagem:") || texto.toLowerCase().contains("gerar imagem")) {
                String aviso = "Para gerar imagens, por favor acesse a aba '🎨 Imagens' no menu lateral esquerdo!";
                adicionarBalao(chatContainer, aviso, false);
                conversaAtual.getMensagens().add(new Mensagem(aviso, false));

                conversaRepository.salvar(conversaAtual);
                carregarConversasDoBanco();
                return;
            }

            Label lblDigitando = new Label("Luna IA está pensando...");
            lblDigitando.setStyle(estiloTexto(13, false, true) + " -fx-font-style: italic; -fx-padding: 5px;");
            chatContainer.getChildren().add(lblDigitando);

            final List<Mensagem> historico = new ArrayList<>(conversaAtual.getMensagens());
            final String personalidadeNaHora = personalidadeIA;
            final String idiomaNaHora = idioma;
            final String contextoRAG = textoDocumento;
            final Conversas conversaDaResposta = conversaAtual;

            Task<String> taskGroq = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiService.gerarRespostaComRAG(historico, personalidadeNaHora, idiomaNaHora, contextoRAG);
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

        txtMensagem.setOnAction(e -> btnEnviar.fire());

        VBox inputContainer = new VBox(4, lblNomeDocumento, new HBox(10, btnAnexarDoc, txtMensagem, btnEnviar));
        inputContainer.setPadding(new Insets(10, 15, 15, 15));
        HBox.setHgrow(inputContainer, Priority.ALWAYS);
        HBox inputBar = new HBox(inputContainer);
        inputBar.setStyle(ThemeManager.getInputBarStyle());
        inputBar.setAlignment(Pos.CENTER);

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollChat);
        layout.setBottom(inputBar);
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

    private boolean ehConversaAtual(Conversas c) {
        if (conversaAtual == null) return false;
        if (c == conversaAtual) return true;
        return c.getId() != null && c.getId().equals(conversaAtual.getId());
    }

    private void atualizarListaConversas(List<Conversas> conversas) {
        listaConversasContainer.getChildren().clear();

        if (conversas.isEmpty()) {
            Label lblVazio = new Label("Nenhuma conversa.");
            lblVazio.setStyle(estiloTexto(12, false, true) + " -fx-padding: 8px;");
            listaConversasContainer.getChildren().add(lblVazio);
            return;
        }

        for (Conversas c : conversas) {
            final boolean ativa = ehConversaAtual(c);

            HBox itemBox = new HBox(6);
            itemBox.setAlignment(Pos.CENTER_LEFT);
            ThemeManager.instalarHover(itemBox,
                    () -> ThemeManager.getBotaoSidebarStyle(ativa, false),
                    () -> ThemeManager.getBotaoSidebarStyle(ativa, true));

            Label lblTitulo = new Label((c.isFavorito() ? "★ " : "") + c.getTitulo());
            lblTitulo.setStyle(estiloTexto(13, ativa, !ativa));
            HBox.setHgrow(lblTitulo, Priority.ALWAYS);

            itemBox.setOnMouseClicked(e -> {
                conversaAtual = c;
                carregarMensagensNaTela();
                mostrarTela(() -> layoutChatAtual);
                aplicarFiltrosEAtualizar();
            });

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.setStyle(ThemeManager.getControlesStyle());

            MenuItem menuFavorito = new MenuItem(c.isFavorito() ? "Remover dos Favoritos" : "Favoritar");
            menuFavorito.setOnAction(e -> {
                c.setFavorito(!c.isFavorito());
                conversaRepository.salvar(c);
                carregarConversasDoBanco();
            });

            MenuItem menuRenomear = new MenuItem("Renomear");
            menuRenomear.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(c.getTitulo());
                dialog.setTitle("Editar Conversa");
                dialog.setHeaderText("Renomear título da conversa:");
                dialog.setContentText("Novo título:");
                aplicarTemaDialogo(dialog);

                dialog.showAndWait().ifPresent(novoTitulo -> {
                    if (!novoTitulo.isBlank()) {
                        c.setTitulo(novoTitulo);
                        conversaRepository.salvar(c);
                        carregarConversasDoBanco();
                    }
                });
            });

            MenuItem menuDeletar = new MenuItem("Deletar");
            menuDeletar.setStyle("-fx-text-fill: " + ThemeManager.getCorErro() + ";");
            menuDeletar.setOnAction(e -> {
                if (c.getId() != null) {
                    conversaRepository.deletar(c.getId());
                    if (conversaAtual != null && conversaAtual.getId() != null && conversaAtual.getId().equals(c.getId())) {
                        iniciarNovaConversaVazia();
                    }
                    carregarConversasDoBanco();
                }
            });

            contextMenu.getItems().addAll(menuFavorito, menuRenomear, menuDeletar);

            itemBox.setOnContextMenuRequested(e -> contextMenu.show(itemBox, e.getScreenX(), e.getScreenY()));

            itemBox.getChildren().addAll(lblTitulo);
            listaConversasContainer.getChildren().add(itemBox);
        }
    }

    private void adicionarBalao(VBox container, String textoOriginal, boolean isUsuario) {
        String textoLimpo = isUsuario ? textoOriginal : textoOriginal.replace("**", "").replace("__", "");

        Label label = new Label(textoLimpo);
        label.setWrapText(true);
        label.setMaxWidth(480);
        label.setStyle(isUsuario ? ThemeManager.getBalaoUsuarioStyle() : ThemeManager.getBalaoIAGetStyle());

        VBox conteudoBalao = new VBox(5);
        conteudoBalao.getChildren().add(label);

        if (!isUsuario) {
            Button btnCopiar = new Button("📋 Copiar");
            ThemeManager.instalarHover(btnCopiar,
                    () -> ThemeManager.getBotaoTextoStyle(false),
                    () -> ThemeManager.getBotaoTextoStyle(true));

            btnCopiar.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(textoLimpo);
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
        wrapper.setAlignment(isUsuario ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        container.getChildren().add(wrapper);
    }
}