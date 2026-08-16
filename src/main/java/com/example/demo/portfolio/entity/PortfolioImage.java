package com.example.demo.portfolio.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PortfolioImage {
  private Integer imageId; //이미지ID
  private Integer portfolioId; 
  private int pageNo; //페이지 번호
  private String pageFeedback; // 페이지별 AI 피드백
  private LocalDateTime createdAt; //생성일
}
