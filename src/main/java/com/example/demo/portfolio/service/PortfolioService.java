package com.example.demo.portfolio.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.portfolio.PortfolioPageAnalysisAgent;
import com.example.demo.ai.portfolio.PortfolioSummaryAnalysisAgent;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dto.request.PortfolioCreateRequest;
import com.example.demo.portfolio.dto.response.PortfolioListResponse;
import com.example.demo.portfolio.dto.response.PortfolioPageFeedbackResponse;
import com.example.demo.portfolio.dto.response.PortfolioSummaryResponse;
import com.example.demo.portfolio.entity.Portfolio;
import com.example.demo.portfolio.entity.PortfolioImage;

@Service
public class PortfolioService {

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private PortfolioPageAnalysisAgent portfolioPageAnalysisAgent;

  @Autowired
  private PortfolioSummaryAnalysisAgent portfolioSummaryAnalysisAgent;

  @Transactional
  public Integer createPortfolio(PortfolioCreateRequest request) throws Exception {

    MultipartFile pdfFile = request.getPdfFile();

    // 포트폴리오 저장
    Portfolio portfolio = new Portfolio();
    portfolio.setMemberId(request.getMemberId());
    portfolio.setTitle(request.getTitle());
    portfolio.setOriginalFilename(pdfFile.getOriginalFilename());
    portfolio.setContentType(pdfFile.getContentType());
    portfolio.setPdfFile(pdfFile.getBytes());

    portfolioDao.insertPortfolio(portfolio);

    return portfolio.getPortfolioId();
  }

  // 페이지별로 분석하는 에이전트 호출
  public List<PortfolioPageFeedbackResponse> analyzePortfolio(Integer portfolioId) throws Exception {
    Portfolio portfolio = portfolioDao.selectPortfolioById(portfolioId);
    return portfolioPageAnalysisAgent.analyzePortfolio(portfolio.getPdfFile(), portfolioId, portfolio.getMemberId());
  }

  // 최종 피드백 생성
  public PortfolioSummaryResponse  generateSummary(Integer portfolioId) throws Exception {
    Portfolio portfolio = portfolioDao.selectPortfolioById(portfolioId);
    return portfolioSummaryAnalysisAgent.generateSummary(portfolioId, portfolio.getMemberId());
  }

  // PDF 조회
  public byte[] getPdf(Integer portfolioId) {
    Portfolio portfolio = portfolioDao.selectPortfolioById(portfolioId);
    return portfolio.getPdfFile();
  }

  // 사용자별 포트폴리오 전체 조회
  public List<Portfolio> getPortfolioList(Integer memberId) {
    return portfolioDao.selectPortfoliosByMemberId(memberId);
  }

  // 포트폴리오 하나 조회
  public Portfolio getPortfolioDetail(Integer portfolioId) {
    return portfolioDao.selectPortfolioById(portfolioId);
  }

  // 포트폴리오 피드백 조회
  public List<PortfolioImage> getPageFeedback(Integer portfolioId) {
    return portfolioImageDao.selectImagesByPortfolioId(portfolioId);
  }

  // 포트폴리오 삭제
  public void deletePortfolio(Integer portfolioId) {
    portfolioDao.deletePortfolio(portfolioId);
  }
}
