package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class InsightJson {
  private SummaryCard summarycard; // UI 상단 요약 카드
  private List<IndustrySentiment> industrySentiment; // 시장 분위기
  private List<String> marketInsight; // 텍스트 인사이트
  private String finalSummary; // 전체 요약
  private List<WordCloudItem> wordCloud; // 단어 중요도
}
