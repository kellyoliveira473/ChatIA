package org.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

public class AIService {

    // Substitua pela sua chave de API da Groq (começa com gsk_...)
    private static final String GROQ_API_KEY = "SUA_CHAVE_GROQ_AQUI";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Envia a mensagem do utilizador para a API da Groq considerando o tom/personalidade escolhido.
     */
    public String gerarrResposta(String mensagemUsuario, String personalidade) throws Exception {
        // Mensagem de sistema que define o tom/personalidade da Luna IA
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Você é a Luna IA. Responda sempre adotando um tom estritamente " + personalidade + ".");

        // Mensagem enviada pelo utilizador
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", mensagemUsuario);

        JSONArray messagesArray = new JSONArray();
        messagesArray.put(systemMessage);
        messagesArray.put(userMessage);

        // Corpo da requisição utilizando o modelo Llama 3 (llama-3.3-70b-versatile ou llama3-8b-8192)
        JSONObject requestBody = new JSONObject();
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
                JSONObject messageObj = choices.getJSONObject(0).getJSONObject("message");
                return messageObj.getString("content");
            }
        } else {
            System.err.println("Erro na API da Groq (" + response.statusCode() + "): " + response.body());
        }

        return "Desculpe, ocorreu um erro ao se comunicar com a Groq API.";
    }
}