package com.example.demo.portfolio.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PortfolioListResponse {
  private Integer portfolioId;
  private String title;
  private LocalDateTime updatedAt;
}
