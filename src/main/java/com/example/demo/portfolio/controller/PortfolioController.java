package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.portfolio.dto.request.PortfolioCreateRequest;
import com.example.demo.portfolio.dto.response.PortfolioCreateResponse;
import com.example.demo.portfolio.dto.response.PortfolioListResponse;
import com.example.demo.portfolio.dto.response.PortfolioPageFeedbackResponse;
import com.example.demo.portfolio.dto.response.PortfolioSummaryResponse;
import com.example.demo.portfolio.entity.Portfolio;
import com.example.demo.portfolio.entity.PortfolioImage;
import com.example.demo.portfolio.service.PortfolioService;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

  @Autowired
  private PortfolioService portfolioService;

  // PDF 업로드
  @PostMapping("/create")
  public ResponseEntity<?> createPortfolio(
      @RequestParam("memberId") Integer memberId,
      @RequestParam("title") String title,
      @RequestParam("pdfFile") MultipartFile pdfFile) {

    PortfolioCreateRequest request = new PortfolioCreateRequest();
    request.setMemberId(memberId);
    request.setTitle(title);
    request.setPdfFile(pdfFile);

    try {
      Integer portfolioId = portfolioService.createPortfolio(request);

      List<PortfolioPageFeedbackResponse> pages = portfolioService.analyzePortfolio(portfolioId);
      PortfolioSummaryResponse summary = portfolioService.generateSummary(portfolioId);

      // 하나의 DTO로 리턴
      PortfolioCreateResponse response = new PortfolioCreateResponse();
      response.setPortfolioId(portfolioId);
      response.setPages(pages);
      response.setSummary(summary);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.toString());
    }
  }

  // PDF 조회
  @GetMapping("/pdf/{portfolioId}")
  public ResponseEntity<byte[]> getPdf(@PathVariable("portfolioId") Integer portfolioId) {
    byte[] pdfBytes = portfolioService.getPdf(portfolioId);
    return ResponseEntity.ok(pdfBytes);
  }

  // 사용자별 포트폴리오 전체 조회
  @GetMapping("/list/{memberId}")
  public ResponseEntity<List<Portfolio>> getPortfolioList(@PathVariable("memberId") Integer memberId) {
    List<Portfolio> list = portfolioService.getPortfolioList(memberId);
    return ResponseEntity.ok(list);
  }

  // 포트폴리오 하나 조회
  @GetMapping("/{portfolioId}")
  public ResponseEntity<Portfolio> getPortfolio(@PathVariable("portfolioId") Integer portfolioId) {
    Portfolio portfolio = portfolioService.getPortfolioDetail(portfolioId);
    return ResponseEntity.ok(portfolio);
  }

  // 포트폴리오 상세 피드백 조회
  @GetMapping("/detail/{portfolioId}")
  public ResponseEntity<List<PortfolioImage>> getPortfolioImage(@PathVariable("portfolioId") Integer portfolioId) {
    List<PortfolioImage> response = portfolioService.getPageFeedback(portfolioId);
    return ResponseEntity.ok(response);
  }

  // 포트폴리오 삭제
  @DeleteMapping("/delete/{portfolioId}")
  public ResponseEntity<String> deletePortfolio(@PathVariable("portfolioId") Integer portfolioId) {
    portfolioService.deletePortfolio(portfolioId);
    return ResponseEntity.ok("포트폴리오 삭제 완료");
  }
}
