package com.rag.controller;

import com.rag.dto.*;
import com.rag.service.ConversationService;
import com.rag.service.RAGService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;
    private final ConversationService conversationService;

    @PostMapping("/ask")
    public ApiResponse<AnswerResponse> ask(@Valid @RequestBody QuestionRequest request,
                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ApiResponse.error(401, "请先登录");
        try {
            AnswerResponse response = ragService.ask(request.getQuestion(),
                    request.getDocumentId(), request.getConversationId(), request.getProvider());
            conversationService.autoTitleFirstMessage(request.getConversationId(), request.getQuestion());
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(500, "问答失败: " + e.getMessage());
        }
    }

    @PostMapping("/ask/stream")
    public SseEmitter askStream(@Valid @RequestBody QuestionRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) { SseEmitter e = new SseEmitter(); e.completeWithError(new RuntimeException("请先登录")); return e; }
        conversationService.autoTitleFirstMessage(request.getConversationId(), request.getQuestion());
        return ragService.askStream(request.getQuestion(), request.getDocumentId(),
                request.getConversationId(), request.getProvider());
    }

    @GetMapping("/suggestions/{conversationId}")
    public ApiResponse<List<String>> suggestions(@PathVariable Long conversationId,
                                                   @RequestParam(defaultValue = "deepseek") String provider,
                                                   HttpSession session) {
        if (session.getAttribute("userId") == null) return ApiResponse.error(401, "请先登录");
        try { return ApiResponse.success(ragService.getSuggestions(conversationId, provider)); }
        catch (Exception e) { return ApiResponse.error(500, "生成建议失败"); }
    }

    @DeleteMapping("/conversation/{conversationId}/messages")
    public ApiResponse<Void> clearMessages(@PathVariable Long conversationId, HttpSession session) {
        if (session.getAttribute("userId") == null) return ApiResponse.error(401, "请先登录");
        conversationService.clearMessages(conversationId);
        return ApiResponse.success(null);
    }
}
