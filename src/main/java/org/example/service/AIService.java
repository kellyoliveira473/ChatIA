package org.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.example.model.Mensagem;

public class AIService {

    // A chave é lida da variável de ambiente GROQ_API_KEY (nunca escreva a chave no código)
    private static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY");
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-120b";

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2) // Força o uso do HTTP/2
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Envia o histórico completo de conversas para a IA manter a memória do chat.
     */
    public String gerarResposta(List<Mensagem> historico, String personalidade, String idioma) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return "Erro: a variável de ambiente GROQ_API_KEY não está definida.";
        }

        JSONArray messagesArray = new JSONArray();

        // Mensagem de sistema definindo a personalidade e o idioma
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Você é a Luna IA. Responda adotando um tom estritamente " + personalidade
                + ". Responda sempre utilizando o idioma: " + idioma + ".");
        messagesArray.put(systemMessage);

        // Adiciona todo o histórico de mensagens da conversa (Memória)
        if (historico != null) {
            for (Mensagem m : historico) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", m.isUsuario() ? "user" : "assistant");
                msgObj.put("content", m.getTexto());
                messagesArray.put(msgObj);
            }
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("messages", messagesArray);
        requestBody.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY.trim())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)") // Evita bloqueio do CloudFront
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
            System.err.println("--- ERRO GROQ API ---");
            System.err.println("Código HTTP: " + response.statusCode());
            System.err.println("Resposta: " + response.body());
        }

        return "Erro (" + response.statusCode() + "): Falha na comunicação com a API da Groq.";
    }

    /**
     * Melhora a ideia simples do usuário transformando em um prompt detalhado em inglês para o gerador de imagens.
     */
    public String melhorarPromptImagem(String ideiaUsuario) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return ideiaUsuario; // Retorna o original se não houver chave
        }

        JSONArray messagesArray = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Você é um especialista em engenharia de prompts para geração de imagens por IA. "
                + "Transforme a ideia simples fornecida pelo usuário em um prompt visual rico, detalhado e esteticamente incrível em inglês. "
                + "Retorne APENAS o texto do prompt gerado, sem introduções ou explicações.");
        messagesArray.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", ideiaUsuario);
        messagesArray.put(userMessage);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("messages", messagesArray);
        requestBody.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY.trim())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                String promptMelhorado = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                return promptMelhorado.replace("\"", ""); // Remove aspas extras se houver
            }
        }

        return ideiaUsuario; // Fallback caso ocorra algum erro
    }
}