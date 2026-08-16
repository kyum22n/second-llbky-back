package com.example.demo.interview.dto.response;

import lombok.Data;

@Data
// 최종 면접 질문 리스트
public class TotalQuestionResponse {
  private Integer questionId;
  private String questionText;
}
