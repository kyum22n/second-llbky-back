package com.example.demo.resume.dto.response;

import lombok.Data;

@Data
public class ResumeCoachResponse {
  private String summary; // 한 줄 요약 코멘트
  private String strengths; // 잘한 점 요약
  private String improvements; // 개선점 요약
  private String improvedText; // AI 수정본
}
