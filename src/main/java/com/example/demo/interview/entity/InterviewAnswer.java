package com.example.demo.interview.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InterviewAnswer {
  private Integer answerId; //답변 ID
  private Integer questionId; //질문 ID

  private String audioFileName;  //오디오 파일 이름
  private String audioFileType; //오디오 파일 타입
  private byte[] audioFileData; //오디오 파일 데이터

  private String videoFileName; //비디오 파일 이름
  private String videoFileType; //비디오 파일 타입
  private byte[] videoFileData; //비디오 파일 데이터

  private String answerText;  //답변 내용(STT 결과)
  private String answerFeedback;  //답변별 피드백
  
  private LocalDateTime createdAt;  //생성일
  private LocalDateTime updatedAt;  //수정일
}
