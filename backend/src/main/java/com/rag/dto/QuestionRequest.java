package com.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionRequest {
    @NotBlank(message = "问题不能为空")
    private String question;

    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    private String provider = "deepseek";
}
