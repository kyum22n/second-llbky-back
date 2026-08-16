package com.example.demo.newstrend.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Data;
/*
  TrendDataAgent -> TrendAnalysisAgent 사이에 사용하는 내부 DTO(중간 단계 DTO)
*/
@Data
public class TrendDataContext {
  private Integer memberId;
  private String jopGroup;
  private String targetRole;
  private String startDate;
  private String endDate;
  private List<String> keywords; // LLM이 만든 키워드
  private Map<String, Object> rawTrendData; // 트렌드 원본 데이터

  private String metaNews; // 뉴스 2차 요약
  private Map<String, Integer> keywordFrequency; // 뉴스 키워드 빈도
}
