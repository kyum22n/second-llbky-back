package com.example.demo.interview.dto;

import lombok.Data;

// LLM 전달용 면접 질문-답변 조합 DTO 

@Data
public class QuestionAnswerBundle {
    private String question;    // 질문
    private String answerText;  // 답변
    private String answerFeedback;  // 답변별 피드백
}
