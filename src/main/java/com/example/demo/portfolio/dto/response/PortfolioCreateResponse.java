package com.example.demo.portfolio.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class PortfolioCreateResponse {
  private Integer portfolioId;
  private List<PortfolioPageFeedbackResponse> pages; // 페이지별 분석 결과
  private PortfolioSummaryResponse summary; // 최종 종합 분석 결과
}
