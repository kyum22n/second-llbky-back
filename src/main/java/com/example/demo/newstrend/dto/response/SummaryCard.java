package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class SummaryCard {
  private String majorKeyword; // 주목 키워드
  private double avgInterest; // 평균 관심도
  private int keywordCount; // 분석 키워드 수
}
