package com.example.demo.learning.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LearningDay {
  private Integer dayId; // 일별 ID
  private Integer weekId; // 주별 ID
  private int dayNumber; // 일차
  private String title; // 제목
  private String content; // 내용
  private int studyTimeMin; // 학습 시간
  private String status; // 진행 상태
  private String learningDaySummary; // 일별 학습 정리
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
