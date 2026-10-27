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

    private static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY");
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    // Modelo oficial e ativo da Groq
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public String gerarResposta(List<Mensagem> historico, String personalidade, String idioma) throws Exception {
        return gerarRespostaComRAG(historico, personalidade, idioma, null);
    }

    public String gerarRespostaComRAG(List<Mensagem> historico, String personalidade, String idioma, String contextoDocumento) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return "Erro: a variável de ambiente GROQ_API_KEY não está definida.";
        }

        JSONArray messagesArray = new JSONArray();

        StringBuilder conteudoSistema = new StringBuilder();
        conteudoSistema.append("Você é a Luna IA, uma assistente virtual extremamente inteligente, precisa e direta. ")
                .append("Adote um tom ")
                .append(personalidade)
                .append(". Responda sempre utilizando o idioma: ")
                .append(idioma)
                .append(". CONTEXTO TEMPORAL: O ano atual é 2026. Considere dados e fatos atuais até o ano de 2026. ")
                .append("REGRA ESTRITA DE FORMATAÇÃO: NUNCA utilize asteriscos (*), negritos (**) nem marcadores markdown. Escreva apenas texto puro, limpo e contínuo.");

        if (contextoDocumento != null && !contextoDocumento.isBlank()) {
            conteudoSistema.append("\n\n[Contexto extraído do documento do usuário]:\n")
                    .append(contextoDocumento)
                    .append("\nUtilize as informações acima com máxima precisão para responder.");
        }

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", conteudoSistema.toString());
        messagesArray.put(systemMessage);

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
        requestBody.put("temperature", 0.1); // Mantém baixa para evitar alucinações

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
                String respostaBruta = choices.getJSONObject(0).getJSONObject("message").getString("content");
                // Remove qualquer asterisco ou caractere de formatação indesejado
                return respostaBruta.replace("*", "").replace("_", "").replace("#", "").trim();
            }
        } else {
            System.err.println("--- ERRO GROQ API ---");
            System.err.println("Código HTTP: " + response.statusCode());
            System.err.println("Resposta: " + response.body());
        }

        return "Erro (" + response.statusCode() + "): Falha na comunicação com a API da Groq.";
    }

    public String melhorarPromptImagem(String ideiaUsuario) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return ideiaUsuario;
        }

        JSONArray messagesArray = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Você é um especialista em engenharia de prompts para geração de imagens por IA. Transforme a ideia simples fornecida pelo usuário em um prompt visual rico, detalhado e esteticamente incrível em inglês. Retorne APENAS o texto do prompt gerado, sem introduções ou explicações.");
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
                return promptMelhorado.replace("\"", "").replace("*", "");
            }
        }

        return ideiaUsuario;
    }
}