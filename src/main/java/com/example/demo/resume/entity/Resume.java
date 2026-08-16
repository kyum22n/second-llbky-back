package com.example.demo.resume.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Resume {
  private Integer resumeId; //이력서 ID
  private Integer memberId;  //사용자 ID

  private String title; // 이력서 제목
  // JSON
  private String careerInfo; //경력
  private String educationInfo;//이력
  private String skills;//기술
  private String certificates;//자격증
  private String awards;//수상
  private String activities;//활동
  private String resumeFeedback; //이력서 피드백

  private LocalDateTime createdAt;//생성일
  private LocalDateTime updatedAt;//수정일

}