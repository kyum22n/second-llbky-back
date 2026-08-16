package com.example.demo.interview.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InterviewQuestion {
  private Integer questionId; //질문 ID
  private Integer sessionId; //면접 ID

  private String questionText; //질문 내용
  
  private LocalDateTime createdAt;  //생성일
}
