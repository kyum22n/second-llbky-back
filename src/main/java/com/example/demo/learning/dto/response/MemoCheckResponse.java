package com.example.demo.learning.dto.response;

import lombok.Data;

// 일일 학습 메모 검증 응답 DTO

@Data
public class MemoCheckResponse {
    private Boolean isValid;    // 메모와 학습 주제의 연관성
    private String reason;  // 이유
    private String summary; // 메모 내용 핵심 요약
}
