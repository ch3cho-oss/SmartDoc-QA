package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.entity.Conversation;
import com.rag.entity.Document;
import com.rag.repository.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> getStats(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ApiResponse.error(401, "请先登录");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long docCount = documentRepository.countByUserId(userId);
        long convCount = conversationRepository.countByUserId(userId);
        long msgCount = messageRepository.countByUserId(userId);
        long todayDocs = documentRepository.countByUserIdAndUploadTimeAfter(userId, todayStart);
        long todayConvs = conversationRepository.countByUserIdAndCreateTimeAfter(userId, todayStart);
        long todayMsgs = messageRepository.countByUserIdAndCreateTimeAfter(userId, todayStart);
        long totalSize = documentRepository.totalSizeByUserId(userId);

        // 文档状态分布
        long processingCount = documentRepository.countByUserIdAndStatus(userId, Document.DocStatus.PROCESSING);
        long completedCount = documentRepository.countByUserIdAndStatus(userId, Document.DocStatus.COMPLETED);
        long failedCount = documentRepository.countByUserIdAndStatus(userId, Document.DocStatus.FAILED);

        // 最近活动
        List<Map<String, Object>> activities = new ArrayList<>();
        List<Document> recentDocs = documentRepository.findTop5Recent(userId);
        for (Document d : recentDocs) {
            Map<String, Object> act = new HashMap<>();
            act.put("type", "upload");
            act.put("docName", d.getFileName());
            act.put("docId", d.getId());
            act.put("status", d.getStatus().name());
            act.put("time", d.getUploadTime());
            activities.add(act);
        }
        List<Conversation> recentConvs = conversationRepository.findTop5Recent(userId);
        for (Conversation c : recentConvs) {
            Map<String, Object> act = new HashMap<>();
            act.put("type", "conversation");
            act.put("convTitle", c.getTitle());
            act.put("docId", c.getDocumentId());
            act.put("time", c.getCreateTime());
            activities.add(act);
        }
        activities.sort((a, b) -> ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time")));
        if (activities.size() > 8) activities = activities.subList(0, 8);

        // 对话热度 TOP3
        List<Map<String, Object>> hotDocs = new ArrayList<>();
        List<Object[]> topIds = conversationRepository.findTopDocumentIds(userId);
        for (Object[] row : topIds) {
            Long docId = (Long) row[0];
            long cnt = (Long) row[1];
            documentRepository.findById(docId).ifPresent(doc -> {
                Map<String, Object> hd = new HashMap<>();
                hd.put("docId", docId);
                hd.put("docName", doc.getFileName());
                hd.put("conversationCount", cnt);
                hotDocs.add(hd);
            });
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("documentCount", docCount);
        stats.put("conversationCount", convCount);
        stats.put("messageCount", msgCount);
        stats.put("todayDocCount", todayDocs);
        stats.put("todayConvCount", todayConvs);
        stats.put("todayMsgCount", todayMsgs);
        stats.put("totalSize", totalSize);
        stats.put("processingCount", processingCount);
        stats.put("completedCount", completedCount);
        stats.put("failedCount", failedCount);
        stats.put("activities", activities);
        stats.put("hotDocs", hotDocs);
        return ApiResponse.success(stats);
    }
}
