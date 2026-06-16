package com.rag.controller;

import com.rag.dto.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Value("${upload-dir:./uploads}")
    private String uploadDir;

    @Value("${embedding.provider:simple}")
    private String embeddingProvider;

    @Value("${llm.provider:deepseek}")
    private String llmProvider;

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(HttpSession session) {
        if (session.getAttribute("userId") == null) return ApiResponse.error(401, "请先登录");

        Map<String, Object> result = new HashMap<>();

        // 存储状态
        File uploadDirFile = new File(uploadDir);
        result.put("storageOk", uploadDirFile.exists() && uploadDirFile.isDirectory());
        result.put("uploadDir", uploadDirFile.getAbsolutePath());

        // 向量引擎状态
        result.put("vectorOk", true); // Lucene 本地索引
        result.put("vectorType", "simple".equals(embeddingProvider) ? "Lucene 本地索引" : "Chroma 向量数据库");

        // LLM 状态（简化判断）
        result.put("llmOk", true);
        result.put("llmType", "deepseek".equals(llmProvider) ? "DeepSeek Chat" : llmProvider);

        return ApiResponse.success(result);
    }
}
