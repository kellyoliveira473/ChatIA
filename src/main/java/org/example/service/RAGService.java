package org.example.service;

import java.util.ArrayList;
import java.util.List;

public class RAGService {

    private final List<String> chunksDocumentos = new ArrayList<>();

    // Adiciona e divide o documento em pedaços de aprox. 500 caracteres
    public void indexarDocumento(String textoCompleto) {
        if (textoCompleto == null || textoCompleto.isBlank()) return;

        int tamanhoChunk = 500;
        int inicio = 0;
        while (inicio < textoCompleto.length()) {
            int fim = Math.min(inicio + tamanhoChunk, textoCompleto.length());
            chunksDocumentos.add(textoCompleto.substring(inicio, fim));
            inicio = fim;
        }
    }

    // Busca o trecho mais relevante com base na pergunta do usuário
    public String recuperarContextoRelevante(String pergunta) {
        if (chunksDocumentos.isEmpty()) return "";

        String melhorChunk = "";
        int maiorPontuacao = 0;

        String[] palavrasChave = pergunta.toLowerCase().split("\\s+");

        for (String chunk : chunksDocumentos) {
            int pontuacao = 0;
            String chunkLower = chunk.toLowerCase();
            for (String palavra : palavrasChave) {
                if (palavra.length() > 3 && chunkLower.contains(palavra)) {
                    pontuacao++;
                }
            }
            if (pontuacao > maiorPontuacao) {
                maiorPontuacao = pontuacao;
                melhorChunk = chunk;
            }
        }

        // Se encontrar relevância, retorna o contexto enriquecido
        return maiorPontuacao > 0 ? melhorChunk : "";
    }
}