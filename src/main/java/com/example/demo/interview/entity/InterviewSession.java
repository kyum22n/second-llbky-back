package com.example.demo.interview.entity;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class InterviewSession {
  private Integer sessionId;  //면접 ID
  private Integer memberId; //사용자 ID

  private String interviewType;  //면접 유형
  private String targetCompany; //희망 기업

  private String documentFileName;  //서류 파일 이름
  private String documentFileType;  //서류 파일 타입
  private byte[] documentFileData;  //서류 바이너리 데이터

  private String reportFeedback;  //종합 피드백
  
  private LocalDateTime createdAt;  //생성일
  private LocalDateTime updatedAt;  //수정일
}
