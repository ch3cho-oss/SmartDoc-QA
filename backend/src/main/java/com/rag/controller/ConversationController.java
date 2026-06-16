package com.rag.controller;

import com.rag.dto.*;
import com.rag.entity.Conversation;
import com.rag.entity.Message;
import com.rag.service.ConversationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/create")
    public ApiResponse<Conversation> create(@RequestBody Map<String, Object> body,
                                            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");

        Long documentId = Long.valueOf(body.get("documentId").toString());
        String title = (String) body.getOrDefault("title", "新对话");

        Conversation conversation = conversationService.createConversation(userId, documentId, title);
        return ApiResponse.success(conversation);
    }

    @GetMapping("/list")
    public ApiResponse<List<Conversation>> list(@RequestParam(required = false) Long documentId,
                                                 HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");

        return ApiResponse.success(conversationService.getConversations(userId, documentId));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<Message>> messages(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");

        return ApiResponse.success(conversationService.getMessages(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        try {
            conversationService.deleteConversation(id);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rename")
    public ApiResponse<Void> rename(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        String title = body.get("title");
        if (title == null || title.trim().isEmpty()) return ApiResponse.error(400, "标题不能为空");
        conversationService.updateTitle(id, title.trim());
        return ApiResponse.success();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody Map<String, List<Long>> body, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) return ApiResponse.error(400, "参数错误");
        for (Long id : ids) conversationService.deleteConversation(id);
        return ApiResponse.success();
    }

    private Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
