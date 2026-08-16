package com.example.demo.learning.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class LearningDetailResponse {
  private Integer learningId;
  private Integer memberId;
  private String title;
  private String status;
  private List<WeekDetailResponse> weeks;
}
