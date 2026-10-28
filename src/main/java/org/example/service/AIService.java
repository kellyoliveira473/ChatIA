package org.example.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.example.model.Mensagem;

public class AIService {

    private static final String GROQ_API_KEY = limparChave(System.getenv("GROQ_API_KEY"));
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Modelo de texto (llama3-70b-8192 e llama-3.3-70b-versatile foram descontinuados pela Groq)
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
    private static final String MODEL = lerEnv("GROQ_MODEL", DEFAULT_MODEL);

    // Modelo multimodal (aceita imagem). Confirme o ID atual em https://console.groq.com/docs/models
    private static final String DEFAULT_VISION_MODEL = "qwen/qwen3.8-27b";
    private static final String VISION_MODEL = lerEnv("GROQ_VISION_MODEL", DEFAULT_VISION_MODEL);

    private static final int MAX_IMAGENS = 3;
    private static final int LADO_MAX_IMAGEM = 1600;

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            System.err.println("AVISO: GROQ_API_KEY não definida.");
        }
    }

    private static String limparChave(String valor) {
        if (valor == null) return null;
        return valor.trim().replaceAll("^[\"']+|[\"']+$", "").trim();
    }

    private static String lerEnv(String nome, String padrao) {
        String env = System.getenv(nome);
        return (env != null && !env.isBlank()) ? env.trim() : padrao;
    }

    public String gerarResposta(List<Mensagem> historico, String personalidade, String idioma) throws Exception {
        return gerarRespostaComRAG(historico, personalidade, idioma, null, null);
    }

    public String gerarRespostaComRAG(List<Mensagem> historico, String personalidade, String idioma,
                                      String contextoDocumento) throws Exception {
        return gerarRespostaComRAG(historico, personalidade, idioma, contextoDocumento, null);
    }

    public String gerarRespostaComRAG(List<Mensagem> historico, String personalidade, String idioma,
                                      String contextoDocumento, List<File> imagens) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            return "Erro: a variável de ambiente GROQ_API_KEY não está definida.";
        }

        boolean temImagens = imagens != null && !imagens.isEmpty();

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", montarPromptSistema(personalidade, idioma, contextoDocumento, temImagens)));

        if (historico != null) {
            int ultimoIndex = historico.size() - 1;
            for (int i = 0; i < historico.size(); i++) {
                Mensagem m = historico.get(i);
                JSONObject obj = new JSONObject().put("role", m.isUsuario() ? "user" : "assistant");

                if (temImagens && i == ultimoIndex && m.isUsuario()) {
                    obj.put("content", montarConteudoComImagens(m.getTexto(), imagens));
                } else {
                    obj.put("content", m.getTexto());
                }
                messages.put(obj);
            }
        }

        String modeloUsado = temImagens ? VISION_MODEL : MODEL;

        JSONObject body = new JSONObject()
                .put("model", modeloUsado)
                .put("messages", messages)
                .put("temperature", 0.1);

        HttpResponse<String> response = enviar(body);

        if (response.statusCode() == 200) {
            JSONArray choices = new JSONObject(response.body()).getJSONArray("choices");
            if (choices.length() > 0) {
                String bruta = choices.getJSONObject(0).getJSONObject("message").optString("content", "");
                String limpa = limparFormatacao(bruta);
                if (!limpa.isBlank()) return limpa;
            }
        } else {
            System.err.println("--- ERRO GROQ API ---");
            System.err.println("Modelo: " + modeloUsado);
            System.err.println("Código HTTP: " + response.statusCode());
            System.err.println("Resposta: " + response.body());
            return "Não consegui analisar a solicitação (erro " + response.statusCode() + "). "
                    + "Verifique o console para mais detalhes.";
        }
        return "Erro (" + response.statusCode() + "): Falha na comunicação com a API da Groq.";
    }

    public String melhorarPromptImagem(String ideiaUsuario) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) return ideiaUsuario;

        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content",
                        "Você é um especialista em engenharia de prompts para geração de imagens por IA. "
                                + "Transforme a ideia simples do usuário em um prompt visual rico e detalhado, em inglês. "
                                + "Retorne APENAS o texto do prompt, sem introduções ou explicações."))
                .put(new JSONObject().put("role", "user").put("content", ideiaUsuario));

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("messages", messages)
                .put("temperature", 0.7);

        HttpResponse<String> response = enviar(body);
        if (response.statusCode() == 200) {
            JSONArray choices = new JSONObject(response.body()).getJSONArray("choices");
            if (choices.length() > 0) {
                String p = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                return p.replace("\"", "").replace("*", "");
            }
        }
        return ideiaUsuario;
    }

    private String montarPromptSistema(String personalidade, String idioma, String contextoDocumento, boolean temImagens) {
        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        StringBuilder s = new StringBuilder();

        s.append("Você é a Luna IA, uma assistente virtual precisa, confiável e direta. ")
                .append("Tom: ").append(personalidade).append(". ")
                .append("Responda sempre no idioma: ").append(idioma).append(". ")
                .append("A data de hoje é ").append(hoje).append(".\n\n")
                .append("REGRAS DE PRECISÃO:\n")
                .append("1. Responda exatamente o que foi perguntado.\n")
                .append("2. Nunca invente fatos. Seja concisa.\n")
                .append("3. NUNCA use asteriscos, negrito, títulos com # nem marcadores markdown. Escreva texto puro.");

        if (temImagens) {
            s.append("\n\nREGRAS PARA IMAGENS: O usuário anexou imagem(ns). ")
                    .append("Descreva, analise e responda com total fidelidade ao que é visível.");
        }

        if (contextoDocumento != null && !contextoDocumento.isBlank()) {
            s.append("\n\n[Contexto do documento]:\n").append(contextoDocumento);
        }
        return s.toString();
    }

    private JSONArray montarConteudoComImagens(String texto, List<File> imagens) throws Exception {
        JSONArray partes = new JSONArray();
        String pedido = (texto == null || texto.isBlank()) ? "Analise esta imagem." : texto;

        partes.put(new JSONObject().put("type", "text").put("text", pedido));

        int n = 0;
        for (File f : imagens) {
            if (n++ >= MAX_IMAGENS) break;
            String dataUrl = imagemParaDataUrl(f);

            JSONObject imageUrlObj = new JSONObject().put("url", dataUrl);
            partes.put(new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", imageUrlObj));
        }
        return partes;
    }

    private String imagemParaDataUrl(File arquivo) throws Exception {
        BufferedImage original = ImageIO.read(arquivo);
        if (original == null) {
            throw new IllegalArgumentException("Formato de imagem não suportado: " + arquivo.getName());
        }
        int w = original.getWidth(), h = original.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAX_IMAGEM / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * escala));
        int nh = Math.max(1, (int) Math.round(h * escala));

        BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(original, 0, 0, nw, nh, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", out);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private HttpResponse<String> enviar(JSONObject requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String limparFormatacao(String texto) {
        return texto
                .replaceAll("(?s)<think>.*?</think>", "")
                .replace("*", "")
                .replaceAll("(?m)^#+\\s*", "")
                .trim();
    }
}