package com.example.demo.learning.dto.response;

import java.util.List;

import lombok.Data;

@Data
// 학습 플랜 생성 응답 - 주차별
public class AiCreateWeekResponse {
  private int weekId;
  private int weekNumber;
  private String title;
  private String goal;
  private String learningWeekSummary;
  private List<AiCreateDayResponse> days;
}
