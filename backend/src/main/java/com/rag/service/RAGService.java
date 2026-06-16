package com.rag.service;

import com.rag.dto.AnswerResponse;
import com.rag.entity.Document;
import com.rag.entity.Message;
import com.rag.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RAGService {

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;
    private final ChromaService chromaService;
    private final LuceneVectorStore luceneVectorStore;
    private final LLMService llmService;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;

    @Value("${app.top-k}")
    private int topK;

    @Value("${embedding.provider}")
    private String embeddingProvider;

    private List<String> retrieveChunks(String question, Long documentId) {
        List<Float> questionEmbedding = embeddingService.embed(question);
        try {
            if ("simple".equals(embeddingProvider)) {
                return luceneVectorStore.search(documentId, questionEmbedding, topK);
            } else {
                List<Map<String, Object>> results = chromaService.query(questionEmbedding, documentId, topK);
                return results.stream()
                        .map(r -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> meta = (Map<String, Object>) r.get("metadata");
                            return meta != null ? (String) meta.get("chunk_text") : "";
                        })
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, String>> buildChatHistory(Long conversationId) {
        List<Message> recentMessages = conversationService.getRecentMessages(conversationId);
        Collections.reverse(recentMessages);
        return recentMessages.stream()
                .map(m -> Map.of(
                        "role", m.getRole().name().toLowerCase(),
                        "content", m.getContent()
                ))
                .collect(Collectors.toList());
    }

    public AnswerResponse ask(String question, Long documentId, Long conversationId, String provider) {
        List<String> chunks = retrieveChunks(question, documentId);
        if (chunks.isEmpty()) {
            throw new RuntimeException("未检索到相关内容，请确认文档已完成处理");
        }
        List<Map<String, String>> chatHistory = buildChatHistory(conversationId);
        String answer = llmService.generateAnswer(question, chunks, chatHistory, provider != null ? provider : "deepseek");

        conversationService.saveMessage(conversationId, Message.MessageRole.USER, question, null);
        String referencesJson = buildReferencesJson(chunks);
        conversationService.saveMessage(conversationId, Message.MessageRole.ASSISTANT, answer, referencesJson);

        return new AnswerResponse(answer, chunks.toArray(new String[0]));
    }

    /**
     * SSE 流式问答 — 逐字推送 token，done 事件中保存完整答案
     */
    public SseEmitter askStream(String question, Long documentId, Long conversationId, String provider) {
        // 检索
        List<String> chunks = retrieveChunks(question, documentId);
        if (chunks.isEmpty()) {
            SseEmitter errorEmitter = new SseEmitter();
            errorEmitter.completeWithError(new RuntimeException("未检索到相关内容，请确认文档已完成处理"));
            return errorEmitter;
        }

        List<Map<String, String>> chatHistory = buildChatHistory(conversationId);
        String referencesJson = buildReferencesJson(chunks);
        conversationService.saveMessage(conversationId, Message.MessageRole.USER, question, null);

        // 先发送 references 给前端
        SseEmitter emitter = new SseEmitter(180_000L);
        try {
            String refsStr = referencesJson != null ? referencesJson : "[]";
            emitter.send(SseEmitter.event().name("refs").data(refsStr));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 委托 LLMService 流式生成，在 done 回调中保存消息
        llmService.generateAnswerStream(question, chunks, chatHistory,
                fullAnswer -> {
                    conversationService.saveMessage(conversationId, Message.MessageRole.ASSISTANT,
                            fullAnswer, referencesJson);
                },
                emitter, provider != null ? provider : "deepseek"
        );

        return emitter;
    }

    /**
     * 根据对话上下文生成追问建议
     */
    public List<String> getSuggestions(Long conversationId, String provider) {
        List<Message> recentMessages = conversationService.getRecentMessages(conversationId);
        Collections.reverse(recentMessages);
        List<Map<String, String>> chatHistory = recentMessages.stream()
                .map(m -> Map.of("role", m.getRole().name().toLowerCase(), "content", m.getContent()))
                .collect(Collectors.toList());
        String lastAnswer = chatHistory.isEmpty() ? "" :
                chatHistory.get(chatHistory.size() - 1).getOrDefault("content", "");
        return llmService.generateSuggestions(lastAnswer, chatHistory, provider != null ? provider : "deepseek");
    }

    @Async
    public void processDocumentAsync(Long documentId) {
        try {
            Document document = documentService.getDocument(documentId);
            String text = documentService.extractText(document);
            List<String> chunks = documentService.chunkText(text);

            if ("simple".equals(embeddingProvider)) {
                List<List<Float>> embeddings = embeddingService.embedBatch(chunks);
                luceneVectorStore.indexDocument(documentId, chunks, embeddings);
            } else {
                List<List<Float>> embeddings = new ArrayList<>();
                for (String chunk : chunks) {
                    embeddings.add(embeddingService.embed(chunk));
                }
                chromaService.createCollection();
                chromaService.addChunks(documentId, chunks, embeddings);
            }

            documentService.updateStatus(documentId, Document.DocStatus.COMPLETED, chunks.size());
        } catch (Exception e) {
            documentService.updateStatus(documentId, Document.DocStatus.FAILED, 0);
            e.printStackTrace();
        }
    }

    private String buildReferencesJson(List<String> chunks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(chunks.get(i))).append("\"");
        }
        sb.append("]");
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
