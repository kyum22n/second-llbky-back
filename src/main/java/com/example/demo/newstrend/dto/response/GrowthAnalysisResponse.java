package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class GrowthAnalysisResponse {
  private String resumeAdvice; // 이력서(서류) 중심 성장 제안
  private String interviewAdvice; // 면접 중심 성장 제안
  private String learningAdvice; // 학습 중심 성장 제안
}
