package com.example.demo.newstrend.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class JobInsight {
  private Integer insightId; // 인사이트 ID(PK)
  private Integer memberId; // 멤버 ID
  private String analysisJson; // 성장 제안
  private String relatedJobsJson; // 추천 직무, 트렌드, 관련 키워드
  private LocalDateTime createdAt; // 생성일
}