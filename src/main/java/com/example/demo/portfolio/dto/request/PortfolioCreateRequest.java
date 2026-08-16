package com.example.demo.portfolio.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class PortfolioCreateRequest {
  private Integer memberId;
  private String title;
  private MultipartFile pdfFile;

}
