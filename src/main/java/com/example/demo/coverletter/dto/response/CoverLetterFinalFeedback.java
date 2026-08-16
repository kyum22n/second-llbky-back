package com.example.demo.coverletter.dto.response;

import java.util.List;

import lombok.Data;

// 최종 피드백 응답
@Data
public class CoverLetterFinalFeedback {

  //문장 분석
  private int grammarScore;   //문법 점수
  private int readabilityScore;   //가독성 점수
  private int logicFlowScore; //논리 흐름 점수

  private List<String> strength;    //강점

  private List<String> improvement; //개선점
}
