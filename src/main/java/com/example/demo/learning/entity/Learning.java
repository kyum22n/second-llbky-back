package com.example.demo.learning.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Learning {
  private Integer learningId; // 학습 ID
  private Integer memberId; // 사용자 ID
  private String title; // 제목
  private String status; // 진행 상태
  private String learningSummary; // 학습 요약
  private String learningRecommendation; // 학습 추천
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
