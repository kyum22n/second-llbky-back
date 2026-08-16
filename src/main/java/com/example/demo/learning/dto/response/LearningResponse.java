package com.example.demo.learning.dto.response;

import lombok.Data;

@Data
public class LearningResponse {
  private Integer learningId;
  private Integer memberId;
  private String title;
  private String status;
}
