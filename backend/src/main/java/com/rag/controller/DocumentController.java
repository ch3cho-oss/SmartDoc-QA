package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.entity.Document;
import com.rag.service.DocumentService;
import com.rag.service.LuceneVectorStore;
import com.rag.service.RAGService;
import com.rag.repository.ConversationRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final RAGService ragService;
    private final LuceneVectorStore luceneVectorStore;
    private final ConversationRepository conversationRepository;

    @PostMapping("/upload")
    public ApiResponse<Document> upload(@RequestParam("file") MultipartFile file,
                                        HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");

        try {
            Document document = documentService.uploadAndParse(userId, file);
            ragService.processDocumentAsync(document.getId());
            return ApiResponse.success(document);
        } catch (Exception e) {
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<Document>> list(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(documentService.getUserDocuments(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Document> get(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        try {
            Document doc = documentService.getDocument(id);
            if (!doc.getUserId().equals(userId)) {
                return ApiResponse.error(403, "无权访问该文档");
            }
            return ApiResponse.success(doc);
        } catch (Exception e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        try {
            Document doc = documentService.getDocument(id);
            if (!doc.getUserId().equals(userId)) {
                return ApiResponse.error(403, "无权访问该文档");
            }
            // 删除该文档下的所有对话（级联删除消息）
            conversationRepository.deleteByDocumentId(id);
            // 清理 Lucene 向量索引
            luceneVectorStore.deleteDocumentIndex(id);
            // 删除上传的文件
            try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (Exception ignored) {}
            // 删除数据库记录
            documentService.deleteDocument(id);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reprocess")
    public ApiResponse<Void> reprocess(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        try {
            Document doc = documentService.getDocument(id);
            if (!doc.getUserId().equals(userId)) {
                return ApiResponse.error(403, "无权访问该文档");
            }
            // 清理旧索引
            luceneVectorStore.deleteDocumentIndex(id);
            // 重置状态并重新处理
            documentService.updateStatus(id, Document.DocStatus.PROCESSING, null);
            ragService.processDocumentAsync(id);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(500, "重新处理失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rename")
    public ApiResponse<Void> renameDoc(@PathVariable Long id,
                                        @RequestBody Map<String, String> body,
                                        HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ApiResponse.error(401, "请先登录");
        String name = body.get("fileName");
        if (name == null || name.trim().isEmpty()) return ApiResponse.error(400, "名称不能为空");
        Document doc = documentService.getDocument(id);
        if (!doc.getUserId().equals(userId)) return ApiResponse.error(403, "无权访问");
        doc.setFileName(name.trim());
        documentService.updateDocument(doc);
        return ApiResponse.success();
    }

    private Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
