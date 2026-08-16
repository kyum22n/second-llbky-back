package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class NewsSecondSummaryResponse {
  private String metaSummary; // 2차 요약문 DB 저장 X LLM 전달용
}
