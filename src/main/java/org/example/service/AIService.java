package org.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

public class AIService {

    // ⚠️ COLE SUA CHAVE DA GROQ AQUI (deve começar com gsk_...)
    private static final String GROQ_API_KEY =
            System.getenv("GROQ_API_KEY");
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public String gerarrResposta(String mensagemUsuario, String personalidade) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return "Erro: Configure a variável de ambiente GROQ_API_KEY!";
        }
        // Prompt de instrução para definir o tom da Luna IA
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Você é a Luna IA. Responda adotando um tom estritamente " + personalidade + ".");

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", mensagemUsuario);

        JSONArray messagesArray = new JSONArray();
        messagesArray.put(systemMessage);
        messagesArray.put(userMessage);

        JSONObject requestBody = new JSONObject();
        // Modelo atualizado e ativo da Groq
        requestBody.put("model", "llama-3.3-70b-versatile");
        requestBody.put("messages", messagesArray);
        requestBody.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content");
            }
        } else {
            // Imprime no console para facilitar o diagnóstico
            System.err.println("--- ERRO GROQ API ---");
            System.err.println("Código HTTP: " + response.statusCode());
            System.err.println("Resposta: " + response.body());
        }

        return "Erro (" + response.statusCode() + "): Falha ao comunicar com a Groq API.";
    }
}