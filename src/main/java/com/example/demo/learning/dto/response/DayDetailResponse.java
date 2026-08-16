package com.example.demo.learning.dto.response;

import lombok.Data;

@Data
public class DayDetailResponse {
  private Integer dayId;
  private Integer dayNumber;
  private String title;
  private String content;
  private String status;
  private String learningDaySummary;
}
