package com.example.demo.learning.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LearningWeek {
  private Integer weekId; // 주별 ID
  private Integer learningId; // 학습 ID
  private int weekNumber; // 주차
  private String title; // 제목
  private String goal; // 목표
  private String status; // 진행 상태
  private String learningWeekSummary; // 주별 학습 요약
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
