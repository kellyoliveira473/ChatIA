package org.example.theme;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * Gerencia o tema (claro/escuro) e gera os estilos inline do JavaFX.
 * Requer Java 16+ (usa record).
 */
public final class ThemeManager {

    // ------------------------------------------------------------------
    //  Paletas
    // ------------------------------------------------------------------

    /** Conjunto de cores de um tema. Todo estilo abaixo é derivado dela. */
    public record Paleta(
            String fundo,             // fundo da janela
            String sidebar,           // fundo da barra lateral
            String superficie,        // balões da IA, campos de texto
            String superficieElevada, // hover e item ativo
            String borda,
            String texto,
            String textoSecundario,
            String placeholder,
            String destaque,          // cor de ação (botão, balão do usuário)
            String destaqueHover,
            String textoSobreDestaque,
            String sombra
    ) {}

    public enum Tema {
        ESCURO(new Paleta(
                "#0b1120", "#0f172a", "#1a2338", "#243049", "#263247",
                "#e8edf6", "#8b98b1", "#64718a",
                "#6d8dff", "#8aa5ff", "#0b1120",
                "rgba(0,0,0,0.35)")),

        CLARO(new Paleta(
                "#f6f8fb", "#eaeef5", "#ffffff", "#e1e7f1", "#d8e0ec",
                "#0f172a", "#5b6980", "#8896ab",
                "#3b5bdb", "#2f4bc4", "#ffffff",
                "rgba(15,23,42,0.10)"));

        private final Paleta paleta;

        Tema(Paleta paleta) {
            this.paleta = paleta;
        }

        public Paleta paleta() {
            return paleta;
        }
    }

    // ------------------------------------------------------------------
    //  Estado
    // ------------------------------------------------------------------

    private static final String FONTE =
            "-fx-font-family: 'Inter', 'Segoe UI', 'SF Pro Text', 'Helvetica Neue', sans-serif;";

    private static final List<Consumer<Tema>> ouvintes = new CopyOnWriteArrayList<>();

    // A escolha do usuário fica salva e é restaurada na próxima abertura do app.
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String CHAVE_TEMA = "tema";

    private static volatile Tema temaAtual = carregarTemaSalvo();

    private ThemeManager() {
    }

    private static Tema carregarTemaSalvo() {
        try {
            return Tema.valueOf(PREFS.get(CHAVE_TEMA, Tema.ESCURO.name()));
        } catch (Exception e) {
            return Tema.ESCURO;
        }
    }

    private static void salvarTema(Tema tema) {
        try {
            PREFS.put(CHAVE_TEMA, tema.name());
        } catch (Exception ignorada) {
            // Se não der para salvar, o tema continua valendo até fechar o app.
        }
    }

    public static Tema getTemaAtual() {
        return temaAtual;
    }

    public static void setTemaAtual(Tema tema) {
        if (tema == null || tema == temaAtual) {
            return;
        }
        temaAtual = tema;
        salvarTema(tema);
        ouvintes.forEach(ouvinte -> ouvinte.accept(tema));
    }

    public static void alternarTema() {
        setTemaAtual(temaAtual == Tema.ESCURO ? Tema.CLARO : Tema.ESCURO);
    }

    /** Registra um callback chamado sempre que o tema mudar (ex.: para reaplicar estilos). */
    public static void aoMudarTema(Consumer<Tema> ouvinte) {
        ouvintes.add(ouvinte);
    }

    public static void removerOuvinte(Consumer<Tema> ouvinte) {
        ouvintes.remove(ouvinte);
    }

    private static Paleta p() {
        return temaAtual.paleta();
    }

    /** Acesso às cores do tema atual (para quem precisa montar um estilo próprio). */
    public static Paleta getPaleta() {
        return p();
    }

    private static String css(String... declaracoes) {
        return String.join(" ", declaracoes);
    }

    // ------------------------------------------------------------------
    //  Janela e layout
    // ------------------------------------------------------------------

    public static String getRootStyle() {
        return css(
                FONTE,
                "-fx-background-color: " + p().fundo() + ";",
                "-fx-background-radius: 14px;",
                "-fx-border-color: " + p().borda() + ";",
                "-fx-border-width: 1px;",
                "-fx-border-radius: 14px;"
        );
    }

    /**
     * Variáveis do JavaFX que fazem os controles nativos (ComboBox, menus de contexto,
     * barras de rolagem, RadioButton, Separator) seguirem o tema. Aplique no nó raiz
     * e em diálogos.
     */
    public static String getControlesStyle() {
        return css(
                "-fx-base: " + p().superficie() + ";",
                "-fx-background: " + p().fundo() + ";",
                "-fx-control-inner-background: " + p().superficie() + ";",
                "-fx-accent: " + p().destaque() + ";",
                "-fx-focus-color: " + p().destaque() + ";",
                "-fx-faint-focus-color: transparent;"
        );
    }

    public static String getDialogStyle() {
        return css(
                FONTE,
                getControlesStyle(),
                "-fx-background-color: " + p().fundo() + ";"
        );
    }

    public static String getSidebarStyle(boolean minimizada) {
        int largura = minimizada ? 68 : 270;
        return css(
                "-fx-background-color: " + p().sidebar() + ";",
                "-fx-background-radius: 14px 0 0 14px;",
                "-fx-border-color: transparent " + p().borda() + " transparent transparent;",
                "-fx-border-width: 0 1px 0 0;",
                "-fx-padding: 12px;",
                "-fx-min-width: " + largura + "px;",
                "-fx-max-width: " + largura + "px;"
        );
    }

    public static String getHeaderStyle() {
        return css(
                "-fx-background-color: transparent;",
                "-fx-border-color: transparent transparent " + p().borda() + " transparent;",
                "-fx-border-width: 0 0 1px 0;",
                "-fx-padding: 14px 24px;"
        );
    }

    public static String getInputBarStyle() {
        return css(
                "-fx-background-color: " + p().sidebar() + ";",
                "-fx-border-color: " + p().borda() + " transparent transparent transparent;",
                "-fx-border-width: 1px 0 0 0;"
        );
    }

    public static String getScrollPaneStyle() {
        return css(
                "-fx-background: transparent;",
                "-fx-background-color: transparent;",
                "-fx-border-color: transparent;",
                "-fx-padding: 0;"
        );
    }

    /** Cartão (estatísticas, lista de opções, resposta da biblioteca). Sem padding: use setPadding. */
    public static String getCardStyle() {
        return css(
                "-fx-background-color: " + p().superficie() + ";",
                "-fx-background-radius: 12px;",
                "-fx-border-color: " + p().borda() + ";",
                "-fx-border-width: 1px;",
                "-fx-border-radius: 12px;"
        );
    }

    // ------------------------------------------------------------------
    //  Texto
    // ------------------------------------------------------------------

    public static String getTextColor() {
        return p().texto();
    }

    public static String getSecondaryTextColor() {
        return p().textoSecundario();
    }

    public static String getAccentColor() {
        return p().destaque();
    }

    public static String getCorSucesso() {
        return temaAtual == Tema.ESCURO ? "#4ade80" : "#16a34a";
    }

    public static String getCorErro() {
        return temaAtual == Tema.ESCURO ? "#f87171" : "#dc2626";
    }

    public static String getStatusStyle(boolean erro) {
        return css(
                FONTE,
                "-fx-text-fill: " + (erro ? getCorErro() : getCorSucesso()) + ";",
                "-fx-font-size: 12px;"
        );
    }

    public static String getTituloStyle() {
        return css(
                FONTE,
                "-fx-text-fill: " + p().texto() + ";",
                "-fx-font-size: 16px;",
                "-fx-font-weight: 600;"
        );
    }

    public static String getTituloGrandeStyle() {
        return css(
                FONTE,
                "-fx-text-fill: " + p().texto() + ";",
                "-fx-font-size: 22px;",
                "-fx-font-weight: 700;"
        );
    }

    public static String getSubtituloStyle() {
        return css(
                FONTE,
                "-fx-text-fill: " + p().textoSecundario() + ";",
                "-fx-font-size: 12px;"
        );
    }

    // ------------------------------------------------------------------
    //  Balões de chat
    // ------------------------------------------------------------------

    public static String getBalaoIAGetStyle() {
        return css(
                FONTE,
                "-fx-background-color: " + p().superficie() + ";",
                "-fx-text-fill: " + p().texto() + ";",
                "-fx-font-size: 14px;",
                "-fx-line-spacing: 3px;",
                "-fx-padding: 12px 16px;",
                "-fx-background-radius: 18px 18px 18px 4px;",
                "-fx-border-color: " + p().borda() + ";",
                "-fx-border-width: 1px;",
                "-fx-border-radius: 18px 18px 18px 4px;"
        );
    }

    public static String getBalaoUsuarioStyle() {
        return css(
                FONTE,
                "-fx-background-color: " + p().destaque() + ";",
                "-fx-text-fill: " + p().textoSobreDestaque() + ";",
                "-fx-font-size: 14px;",
                "-fx-line-spacing: 3px;",
                "-fx-padding: 12px 16px;",
                "-fx-background-radius: 18px 18px 4px 18px;"
        );
    }

    // ------------------------------------------------------------------
    //  Campo de texto
    // ------------------------------------------------------------------

    public static String getTextFieldStyle() {
        return css(
                FONTE,
                "-fx-background-color: " + p().superficie() + ";",
                "-fx-text-fill: " + p().texto() + ";",
                "-fx-prompt-text-fill: " + p().placeholder() + ";",
                "-fx-highlight-fill: " + p().destaque() + ";",
                "-fx-highlight-text-fill: " + p().textoSobreDestaque() + ";",
                "-fx-font-size: 14px;",
                "-fx-padding: 12px 16px;",
                "-fx-background-radius: 12px;",
                "-fx-border-color: " + p().borda() + ";",
                "-fx-border-width: 1px;",
                "-fx-border-radius: 12px;"
        );
    }

    public static String getTextFieldFocusedStyle() {
        return css(
                getTextFieldStyle(),
                "-fx-border-color: " + p().destaque() + ";",
                "-fx-effect: dropshadow(gaussian, " + p().destaque() + "66, 10, 0, 0, 0);"
        );
    }

    // ------------------------------------------------------------------
    //  Botões
    // ------------------------------------------------------------------

    public static String getBotaoEnviarStyle(boolean hover) {
        return css(
                FONTE,
                "-fx-background-color: " + (hover ? p().destaqueHover() : p().destaque()) + ";",
                "-fx-text-fill: " + p().textoSobreDestaque() + ";",
                "-fx-font-size: 14px;",
                "-fx-font-weight: 600;",
                "-fx-padding: 11px 20px;",
                "-fx-background-radius: 12px;",
                "-fx-cursor: hand;",
                "-fx-effect: dropshadow(gaussian, " + p().sombra() + ", 8, 0, 0, 2);"
        );
    }

    /** Botão neutro (anexar, voltar, alternar tema nas configurações). */
    public static String getBotaoSecundarioStyle(boolean hover) {
        return css(
                FONTE,
                "-fx-background-color: " + (hover ? p().superficieElevada() : p().superficie()) + ";",
                "-fx-text-fill: " + p().texto() + ";",
                "-fx-font-size: 13px;",
                "-fx-padding: 10px 16px;",
                "-fx-background-radius: 10px;",
                "-fx-border-color: " + p().borda() + ";",
                "-fx-border-width: 1px;",
                "-fx-border-radius: 10px;",
                "-fx-cursor: hand;"
        );
    }

    /** Botão de ação destrutiva (sair da conta). */
    public static String getBotaoPerigoStyle(boolean hover) {
        return css(
                FONTE,
                "-fx-background-color: " + (hover ? "#dc2626" : "#ef4444") + ";",
                "-fx-text-fill: #ffffff;",
                "-fx-font-size: 13px;",
                "-fx-font-weight: 600;",
                "-fx-padding: 10px 16px;",
                "-fx-background-radius: 10px;",
                "-fx-cursor: hand;"
        );
    }

    /** Botão só com texto (ex.: "Copiar" embaixo do balão). */
    public static String getBotaoTextoStyle(boolean hover) {
        return css(
                FONTE,
                "-fx-background-color: transparent;",
                "-fx-text-fill: " + (hover ? p().texto() : p().textoSecundario()) + ";",
                "-fx-font-size: 11px;",
                "-fx-padding: 2px 0 0 0;",
                "-fx-cursor: hand;"
        );
    }

    /** Filtro "Todas / Favoritas" da barra lateral. */
    public static String getBotaoFiltroStyle(boolean ativo) {
        return css(
                FONTE,
                "-fx-background-color: " + (ativo ? p().destaque() : p().superficie()) + ";",
                "-fx-text-fill: " + (ativo ? p().textoSobreDestaque() : p().textoSecundario()) + ";",
                "-fx-font-size: 11px;",
                "-fx-font-weight: " + (ativo ? "600" : "normal") + ";",
                "-fx-padding: 6px;",
                "-fx-background-radius: 8px;",
                "-fx-border-color: " + (ativo ? "transparent" : p().borda()) + ";",
                "-fx-border-width: 1px;",
                "-fx-border-radius: 8px;",
                "-fx-cursor: hand;"
        );
    }

    /**
     * Item da barra lateral.
     *
     * @param ativo se é a conversa/seção selecionada
     * @param hover se o mouse está sobre o item
     */
    public static String getBotaoSidebarStyle(boolean ativo, boolean hover) {
        String fundo = (ativo || hover) ? p().superficieElevada() : "transparent";
        String texto = ativo ? p().texto() : p().textoSecundario();
        if (hover && !ativo) {
            texto = p().texto();
        }
        return css(
                FONTE,
                "-fx-background-color: " + fundo + ";",
                "-fx-text-fill: " + texto + ";",
                "-fx-font-size: 13px;",
                "-fx-font-weight: " + (ativo ? "600" : "normal") + ";",
                "-fx-alignment: center-left;",
                "-fx-padding: 10px 14px;",
                "-fx-background-radius: 10px;",
                "-fx-cursor: hand;"
        );
    }

    // ------------------------------------------------------------------
    //  Barra de título
    // ------------------------------------------------------------------

    public static String getTopBarStyle() {
        return css(
                "-fx-background-color: " + p().sidebar() + ";",
                "-fx-background-radius: 14px 14px 0 0;",
                "-fx-border-color: transparent transparent " + p().borda() + " transparent;",
                "-fx-border-width: 0 0 1px 0;"
        );
    }

    public static String getTituloBarraStyle() {
        return css(
                FONTE,
                "-fx-text-fill: " + p().textoSecundario() + ";",
                "-fx-font-size: 12px;",
                "-fx-font-weight: 600;"
        );
    }

    /**
     * Botão da barra (tema, minimizar, fechar).
     *
     * @param hover  se o mouse está sobre o botão
     * @param fechar true para o botão de fechar (fica vermelho no hover)
     */
    public static String getBotaoJanelaStyle(boolean hover, boolean fechar) {
        String fundo = "transparent";
        String texto = p().textoSecundario();
        if (hover) {
            fundo = fechar ? "#ef4444" : p().superficieElevada();
            texto = fechar ? "#ffffff" : p().texto();
        }
        return css(
                "-fx-background-color: " + fundo + ";",
                "-fx-text-fill: " + texto + ";",
                "-fx-font-size: 13px;",
                "-fx-min-width: 34px;",
                "-fx-min-height: 28px;",
                "-fx-max-height: 28px;",
                "-fx-padding: 0;",
                "-fx-background-radius: 8px;",
                "-fx-cursor: hand;"
        );
    }

    // ------------------------------------------------------------------
    //  Helpers para evitar repetir listeners de hover/foco nos controllers
    // ------------------------------------------------------------------

    /** Aplica o estilo normal e troca para o de hover quando o mouse entra/sai. */
    public static void instalarHover(Node no, Supplier<String> normal, Supplier<String> hover) {
        no.setStyle(normal.get());
        no.setOnMouseEntered(e -> no.setStyle(hover.get()));
        no.setOnMouseExited(e -> no.setStyle(normal.get()));
    }

    /** Estiliza um TextField/TextArea e destaca a borda quando ele ganha foco. */
    public static void instalarCampoTexto(TextInputControl campo) {
        campo.setStyle(getTextFieldStyle());
        campo.focusedProperty().addListener((obs, antes, focado) ->
                campo.setStyle(focado ? getTextFieldFocusedStyle() : getTextFieldStyle()));
    }
}