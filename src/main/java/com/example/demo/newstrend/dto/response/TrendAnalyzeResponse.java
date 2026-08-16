package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class TrendAnalyzeResponse {
  // api 기반 LLM이 계산 후 만들어진 트렌드 그래프 데이터(키워드 목록, 키워드 점수, 원본 데이터(rawdata))
  private TrendGraph trendJson;
  
  // 상단 요약카드 + 키워드 트렌드(변화율) + 워드클라우드 + 산업 분위기 + 전체 요약
  private InsightJson insightJson;
}
