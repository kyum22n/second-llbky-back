package com.example.demo.learning.dto.request;

import java.util.List;

import lombok.Data;

@Data
// 분석한 피드백을 바탕으로 부족한 역량 추천 요청
public class SkillRecommendRequest {
  private List<String> resumeFeedback;
  private List<String> coverLetterFeedback;
  private List<String> portfolioFeedback;
}
