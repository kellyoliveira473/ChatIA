package org.example.theme;

public class ThemeManager {

    public enum Tema {
        ESCURO, CLARO
    }

    private static Tema temaAtual = Tema.ESCURO;

    public static Tema getTemaAtual() {
        return temaAtual;
    }

    public static void setTemaAtual(Tema tema) {
        temaAtual = tema;
    }

    public static String getRootStyle() {
        if (temaAtual == Tema.CLARO) {
            return "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 1px;";
        }
        return "-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 1px;";
    }

    public static String getSidebarStyle(boolean minimizada) {
        String largura = minimizada ? "-fx-min-width: 65px; -fx-max-width: 65px;" : "-fx-min-width: 270px; -fx-max-width: 270px;";
        if (temaAtual == Tema.CLARO) {
            return "-fx-background-color: #e2e8f0; " + largura;
        }
        return "-fx-background-color: #1e293b; " + largura;
    }

    public static String getTextColor() {
        return temaAtual == Tema.CLARO ? "#0f172a" : "#f8fafc";
    }

    public static String getBalaoIAGetStyle() {
        if (temaAtual == Tema.CLARO) {
            return "-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-padding: 10px 16px; -fx-background-radius: 16px 16px 16px 2px; -fx-font-size: 14px;";
        }
        return "-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-padding: 10px 16px; -fx-background-radius: 16px 16px 16px 2px; -fx-font-size: 14px;";
    }

    public static String getTextFieldStyle() {
        if (temaAtual == Tema.CLARO) {
            return "-fx-background-color: #ffffff; -fx-text-fill: #0f172a; -fx-prompt-text-fill: #64748b; -fx-font-size: 14px; -fx-background-radius: 8px; -fx-padding: 10px;";
        }
        return "-fx-background-color: #334155; -fx-text-fill: white; -fx-prompt-text-fill: #94a3b8; -fx-font-size: 14px; -fx-background-radius: 8px; -fx-padding: 10px;";
    }
}