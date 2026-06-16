package com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerResponse {
    private String answer;
    private String[] references;
}
