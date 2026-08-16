package com.example.demo.learning.dto.response;

import lombok.Data;

@Data
// 학습 플랜 생성 응답 - 일차별
public class AiCreateDayResponse {
  private int dayNumber;
  private String title;
  private String content;
}