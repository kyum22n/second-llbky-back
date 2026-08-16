package com.example.demo.portfolio.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class PortfolioSummaryResponse {
  private int finalScore;
  private List<String> strengths;
  private List<String> weaknesses;
  private String visualDesign;
  private String informationStructure;
  private String technicalComposition;
  private String contentQuality;
  private String expression;
  private String overallReview;
}
