package org.example;

/**
 * Classe lançadora. Ela NÃO estende Application, o que evita o erro
 * "JavaFX runtime components are missing" ao rodar a partir de um jar único.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}