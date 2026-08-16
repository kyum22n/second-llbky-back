package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class JobInsightResponse {
  private String jobRole; // 직무
  private String summary; // 직무 요약
  private String trendSummary; // 트렌드 요약
  private List<String> relatedKeywords; // 관련 키워드 목록
}
