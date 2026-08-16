package com.example.demo.interview.dto.response;

import lombok.Data;

// 면접세션 생성 응답
@Data
public class SaveSessionResponse {
  private Integer sessionId;
  private Integer questionId;
  private String questionText;
}
