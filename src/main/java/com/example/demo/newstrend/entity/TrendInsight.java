package com.example.demo.newstrend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TrendInsight {
  private Integer trendId; // 트렌드 ID(PK)
  private Integer memberId; // 멤버 ID

  // 분석한 트렌드 기간
  private LocalDate startDate;
  private LocalDate endDate;

  private String trendJson; // 그래프 데이터
  private String insightJson; // LLM이 분석한 트렌드 페이지 전체 구성 UI 데이터 JSONB
  private LocalDateTime createdAt;
}
