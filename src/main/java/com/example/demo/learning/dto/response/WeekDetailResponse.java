package com.example.demo.learning.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class WeekDetailResponse {
  private Integer weekId;
  private Integer weekNumber;
  private String title;
  private String goal;
  private String status;
  private String weekSummary;
  private List<DayDetailResponse> days;
}
