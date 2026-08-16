package com.example.demo.interview.dto.response;

import lombok.Data;

// AI 답변별 피드백 구조
@Data
public class AnswerFeedbackResponse {
  private int languageScore;  //언어 점수
  private int nonLanguageScore; //비언어 점수
  private int totalScore; //종합 점수

  private String overallSummary;  //종합 요약
  private String keyCoachingPoint; //핵심 코칭 포인트
  
  private String speechAnalysis;  //발음&말투 분석
  private String toneExpressionAnalysis;  //톤&표정 분석
  private String timeStructureAnalysis; //시간&구성 분석
  private String contentAnalysis; //내용 분석
}
