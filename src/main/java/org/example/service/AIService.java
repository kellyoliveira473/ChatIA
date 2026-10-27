package org.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.example.model.Mensagem;

public class AIService {

    private static final String GROQ_API_KEY = limparChave(System.getenv("GROQ_API_KEY"));
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // llama-3.3-70b-versatile foi descontinuado pela Groq (16/08/2026).
    // Substituto recomendado: openai/gpt-oss-120b (alternativa: qwen/qwen3.6-27b).
    // Para trocar sem recompilar, defina a variável de ambiente GROQ_MODEL.
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
    private static final String MODEL = resolverModelo();

    // Pesquisa web (Tavily). Defina a variável de ambiente TAVILY_API_KEY.
    private static final String TAVILY_API_KEY = limparChave(System.getenv("TAVILY_API_KEY"));
    private static final String TAVILY_URL = "https://api.tavily.com/search";
    private static final int MAX_RESULTADOS_WEB = 3;
    private static final int MAX_CHARS_POR_RESULTADO = 600;
    private static final int MAX_CHARS_CONSULTA = 300;

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Diagnóstico temporário (não expõe a chave inteira). Remova depois de funcionar.
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            System.err.println("AVISO: GROQ_API_KEY não definida.");
        } else {
            int n = GROQ_API_KEY.length();
            System.err.println("Groq key: tamanho=" + n
                    + ", início=" + GROQ_API_KEY.substring(0, Math.min(4, n))
                    + ", fim=" + GROQ_API_KEY.substring(Math.max(0, n - 4)));
        }

        if (TAVILY_API_KEY == null || TAVILY_API_KEY.isBlank()) {
            System.err.println("AVISO: TAVILY_API_KEY não definida. A pesquisa na web está desativada.");
        } else {
            // Diagnóstico temporário (não expõe a chave). Remova depois de funcionar.
            System.err.println("Tavily key: tamanho=" + TAVILY_API_KEY.length()
                    + ", início=" + TAVILY_API_KEY.substring(0, Math.min(5, TAVILY_API_KEY.length())));
        }
    }

    // Remove espaços, quebras de linha e aspas que costumam vir junto da variável de ambiente.
    private static String limparChave(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.trim().replaceAll("^[\"']+|[\"']+$", "").trim();
    }

    private static String resolverModelo() {
        String env = System.getenv("GROQ_MODEL");
        return (env != null && !env.isBlank()) ? env.trim() : DEFAULT_MODEL;
    }

    public String gerarResposta(List<Mensagem> historico, String personalidade, String idioma) throws Exception {
        return gerarRespostaComRAG(historico, personalidade, idioma, null);
    }

    public String gerarRespostaComRAG(List<Mensagem> historico, String personalidade, String idioma, String contextoDocumento) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return "Erro: a variável de ambiente GROQ_API_KEY não está definida.";
        }

        // Pesquisa rápida na web antes de responder (falhas não interrompem a resposta)
        String resultadosWeb = pesquisarNaWeb(ultimaPerguntaDoUsuario(historico));

        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        JSONArray messagesArray = new JSONArray();

        StringBuilder conteudoSistema = new StringBuilder();
        conteudoSistema.append("Você é a Luna IA, uma assistente virtual extremamente inteligente, precisa e direta. ")
                .append("Adote um tom ")
                .append(personalidade)
                .append(". Responda sempre utilizando o idioma: ")
                .append(idioma)
                .append(". CONTEXTO TEMPORAL: a data de hoje é ")
                .append(hoje)
                .append(". ")
                .append("REGRA ESTRITA DE FORMATAÇÃO: NUNCA utilize asteriscos (*), negritos (**) nem marcadores markdown. Escreva apenas texto puro, limpo e contínuo.");

        if (resultadosWeb != null) {
            conteudoSistema.append("\n\n[Resultados de pesquisa na web, obtidos agora]:\n")
                    .append(resultadosWeb)
                    .append("\nUse esses resultados como fonte principal para fatos atuais e verificáveis. ")
                    .append("Se eles não responderem à pergunta, diga que não encontrou informação confiável em vez de inventar. ")
                    .append("Quando for útil, mencione o nome do site da fonte em texto puro.");
        }

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

        HttpResponse<String> response = enviar(requestBody);

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                String respostaBruta = choices.getJSONObject(0).getJSONObject("message").getString("content");
                return limparFormatacao(respostaBruta);
            }
        } else {
            System.err.println("--- ERRO GROQ API ---");
            System.err.println("Modelo: " + MODEL);
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

        HttpResponse<String> response = enviar(requestBody);

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                String promptMelhorado = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                return promptMelhorado.replace("\"", "").replace("*", "");
            }
        } else {
            System.err.println("--- ERRO GROQ API (melhorarPromptImagem) ---");
            System.err.println("Modelo: " + MODEL);
            System.err.println("Código HTTP: " + response.statusCode());
            System.err.println("Resposta: " + response.body());
        }

        return ideiaUsuario;
    }

    private HttpResponse<String> enviar(JSONObject requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY.trim())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Retorna a última mensagem enviada pelo usuário no histórico (ou null).
    private String ultimaPerguntaDoUsuario(List<Mensagem> historico) {
        if (historico == null) {
            return null;
        }
        for (int i = historico.size() - 1; i >= 0; i--) {
            Mensagem m = historico.get(i);
            if (m.isUsuario()) {
                return m.getTexto();
            }
        }
        return null;
    }

    // Pesquisa breve na web via Tavily. Retorna null se a pesquisa estiver
    // desativada ou falhar, para que o chat continue respondendo normalmente.
    private String pesquisarNaWeb(String pergunta) {
        if (TAVILY_API_KEY == null || TAVILY_API_KEY.isBlank() || pergunta == null || pergunta.isBlank()) {
            return null;
        }

        try {
            String consulta = pergunta.trim();
            if (consulta.length() > MAX_CHARS_CONSULTA) {
                consulta = consulta.substring(0, MAX_CHARS_CONSULTA);
            }

            JSONObject body = new JSONObject();
            body.put("query", consulta);
            body.put("search_depth", "basic");
            body.put("max_results", MAX_RESULTADOS_WEB);
            body.put("include_answer", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TAVILY_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TAVILY_API_KEY.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("--- ERRO TAVILY ---");
                System.err.println("Código HTTP: " + response.statusCode());
                System.err.println("Resposta: " + response.body());
                return null;
            }

            JSONArray results = new JSONObject(response.body()).optJSONArray("results");
            if (results == null || results.length() == 0) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.length(); i++) {
                JSONObject r = results.getJSONObject(i);
                String conteudo = r.optString("content", "").trim();
                if (conteudo.length() > MAX_CHARS_POR_RESULTADO) {
                    conteudo = conteudo.substring(0, MAX_CHARS_POR_RESULTADO) + "...";
                }
                sb.append(i + 1).append(") ")
                        .append(r.optString("title", "Sem título"))
                        .append(" (").append(r.optString("url", "")).append(")\n")
                        .append(conteudo).append("\n\n");
            }
            return sb.toString().trim();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            System.err.println("Falha na pesquisa web: " + e.getMessage());
            return null;
        }
    }

    // Remove só a formatação markdown (asteriscos e títulos com #),
    // preservando underscores e # legítimos (nomes de variáveis, URLs, hashtags).
    private String limparFormatacao(String texto) {
        return texto
                .replace("*", "")
                .replaceAll("(?m)^#+\\s*", "")
                .trim();
    }
}