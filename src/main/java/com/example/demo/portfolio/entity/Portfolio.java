package com.example.demo.portfolio.entity;


import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Portfolio {
  private Integer portfolioId;//포트폴리오 ID
  private Integer memberId;
  private String title;//포트폴리오 제목
  private byte[] pdfFile; //파일
  private String originalFilename; //원본파일
  private String contentType; //타입
  private int pageCount; //페이지수(PDF만 사용)
  private String portfolioFeedback; //포트폴리오 피드백
  private LocalDateTime createdAt; //생성일
  private LocalDateTime updatedAt; // 수정일
}
