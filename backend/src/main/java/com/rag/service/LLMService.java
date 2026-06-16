package com.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class LLMService {

    private final RestTemplate restTemplate;

    @Value("${llm.deepseek.api-key}")
    private String deepseekKey;

    @Value("${llm.deepseek.model}")
    private String deepseekModel;

    @Value("${llm.deepseek.url}")
    private String deepseekUrl;

    @Value("${llm.kimi.api-key}")
    private String kimiKey;

    @Value("${llm.kimi.model}")
    private String kimiModel;

    @Value("${llm.kimi.url}")
    private String kimiUrl;

    @Value("${llm.temperature}")
    private double temperature;

    @Value("${llm.max-tokens}")
    private int maxTokens;

    public LLMService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private record ProviderConfig(String url, String apiKey, String model) {}

    private ProviderConfig getConfig(String provider) {
        if ("kimi".equals(provider)) {
            return new ProviderConfig(kimiUrl, kimiKey, kimiModel);
        }
        return new ProviderConfig(deepseekUrl, deepseekKey, deepseekModel);
    }

    public String generateAnswer(String question, List<String> chunks,
                                  List<Map<String, String>> chatHistory, String provider) {
        ProviderConfig cfg = getConfig(provider);
        String systemPrompt = "你是一个基于文档的问答助手。请根据以下【参考内容】回答用户的问题。\n\n" +
                "【重要】在回答中引用参考内容时，请使用 [1]、[2]、[3] 等编号标注引用来源（编号对应参考内容的编号）。\n" +
                "例如：「根据[1]，培训时间为2026年3月。」\n\n" +
                "如果参考内容中没有相关信息，请直接说「根据当前文档，无法回答该问题」。";
        String fullPrompt = buildFullPrompt(question, chunks, chatHistory, systemPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", fullPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", cfg.model());
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(cfg.url(), request, Map.class);

        if (response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }
        throw new RuntimeException("LLM API 调用失败，请稍后重试");
    }

    public void generateAnswerStream(String question, List<String> chunks,
                                      List<Map<String, String>> chatHistory,
                                      java.util.function.Consumer<String> onDone,
                                      SseEmitter emitter, String provider) {
        ProviderConfig cfg = getConfig(provider);
        CompletableFuture.runAsync(() -> {
            try {
                String systemPrompt = "你是一个基于文档的问答助手。请根据以下【参考内容】回答用户的问题。\\n\\n" +
                        "【重要】在回答中引用参考内容时，请使用 [1]、[2]、[3] 等编号标注引用来源（编号对应参考内容的编号）。\\n" +
                        "例如：「根据[1]，培训时间为2026年3月。」\\n\\n" +
                        "如果参考内容中没有相关信息，请直接说「根据当前文档，无法回答该问题」。";
                String fullPrompt = buildFullPrompt(question, chunks, chatHistory, systemPrompt);

                StringBuilder jsonBody = new StringBuilder();
                jsonBody.append("{");
                jsonBody.append("\"model\":\"").append(cfg.model()).append("\",");
                jsonBody.append("\"temperature\":").append(temperature).append(",");
                jsonBody.append("\"max_tokens\":").append(maxTokens).append(",");
                jsonBody.append("\"stream\":true,");
                jsonBody.append("\"messages\":[");
                jsonBody.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"},");
                jsonBody.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(fullPrompt)).append("\"}");
                jsonBody.append("]}");

                HttpURLConnection conn = (HttpURLConnection) URI.create(cfg.url()).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + cfg.apiKey());
                conn.setDoOutput(true);
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(120_000);

                conn.getOutputStream().write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
                conn.getOutputStream().flush();

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    emitter.send(SseEmitter.event().name("error")
                            .data("API 返回状态码: " + responseCode));
                    emitter.complete();
                    return;
                }

                StringBuilder fullAnswer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) break;
                            try {
                                String content = extractContent(data);
                                if (content != null && !content.isEmpty()) {
                                    fullAnswer.append(content);
                                    emitter.send(SseEmitter.event().name("token").data(content));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                String answer = fullAnswer.toString();
                emitter.send(SseEmitter.event().name("done").data(answer));
                if (onDone != null) onDone.accept(answer);
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("生成失败: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });
    }

    public List<String> generateSuggestions(String lastAnswer,
                                             List<Map<String, String>> chatHistory,
                                             String provider) {
        ProviderConfig cfg = getConfig(provider);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey());

        StringBuilder historyStr = new StringBuilder();
        if (chatHistory != null && !chatHistory.isEmpty()) {
            for (Map<String, String> msg : chatHistory) {
                historyStr.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }
        }

        String prompt = "对话历史：\n" + historyStr + "\n最新回答：" + lastAnswer +
                "\n\n请根据以上对话内容，生成2-3个用户可能想继续追问的问题。直接返回问题，每行一个，不要编号，不要加任何前缀。";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "你是一个对话分析助手，请根据对话历史生成追问建议。"));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", cfg.model());
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 200);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(cfg.url(), request, Map.class);

        List<String> suggestions = new ArrayList<>();
        if (response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    String content = (String) message.get("content");
                    for (String line : content.split("\n")) {
                        String trimmed = line.trim()
                                .replaceAll("^[\\d\\.\\-\\s]+", "")
                                .replaceAll("^[\"'「『]", "")
                                .replaceAll("[\"'」』]$", "")
                                .trim();
                        if (!trimmed.isEmpty()) suggestions.add(trimmed);
                    }
                }
            }
        }
        return suggestions;
    }

    private String buildFullPrompt(String question, List<String> chunks,
                                    List<Map<String, String>> chatHistory, String systemPrompt) {
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            contextBuilder.append("【参考内容").append(i + 1).append("】\n");
            contextBuilder.append(chunks.get(i)).append("\n\n");
        }
        String context = contextBuilder.toString();

        StringBuilder historyBuilder = new StringBuilder();
        if (chatHistory != null && !chatHistory.isEmpty()) {
            for (Map<String, String> msg : chatHistory) {
                historyBuilder.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }
        }
        String history = historyBuilder.toString();

        String fullPrompt = "参考内容：\n" + context;
        if (!history.isEmpty()) {
            fullPrompt += "\n对话历史：\n" + history;
        }
        fullPrompt += "\n用户问题：" + question + "\n请回答：";
        return fullPrompt;
    }

    private String extractContent(String json) {
        int idx = json.indexOf("\"content\":\"");
        if (idx == -1) return null;
        idx += 11;
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c); break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
