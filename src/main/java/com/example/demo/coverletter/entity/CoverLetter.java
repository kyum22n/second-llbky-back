package com.example.demo.coverletter.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CoverLetter {
  private Integer coverletterId;  //자소서 ID
  private Integer memberId; //사용자 ID

  private String title; //자소서 제목
  private String supportMotive; //지원동기
  private String growthExperience;  //성장과정
  private String jobCapability; //직무역량
  private String futurePlan;  //입사 후 포부

  private String coverFeedback;  // 자소서 피드백(JSONB)

  private LocalDateTime createdAt;  //생성일
  private LocalDateTime updatedAt;  //수정일
}
