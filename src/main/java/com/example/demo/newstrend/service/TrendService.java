package com.example.demo.newstrend.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.newstrend.TrendAnalysisAgent;
import com.example.demo.ai.newstrend.TrendDataAgent;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.InsightJson;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.dto.response.TrendDataContext;
import com.example.demo.newstrend.dto.response.TrendGraph;
import com.example.demo.newstrend.entity.TrendInsight;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TrendService {
  @Autowired
  private TrendAnalysisAgent trendAnalysisAgent;
  @Autowired
  private TrendDataAgent trendDataAgent;
  @Autowired
  private TrendInsightDao trendInsightDao;
  @Autowired
  private ObjectMapper mapper;
  
  // 오늘 데이터 조회 없으면 자동 분석
  public TrendAnalyzeResponse getAnalyzeToday(Integer memberId) throws Exception {
    LocalDate today = LocalDate.now();
    TrendInsight latest = trendInsightDao.selectLatestTrendInsight(memberId);
    
    // 오늘 분석 데이터가 있으면 DB값 반환
    if (latest != null && latest.getCreatedAt() != null && latest.getCreatedAt().toLocalDate().isEqual(today)) {
      return entityToResponse(latest);
    }
    // 오늘 데이터 없을시 분석
    // 트렌드 원본 데이터 수집
    TrendDataContext context = trendDataAgent.collect(memberId);
  
    // 원본 데이터 넘겨서 LLM 분석 실행
    TrendAnalyzeResponse response = trendAnalysisAgent.analyze(context);
  
    // DB 저장
    TrendInsight entity = new TrendInsight();
    entity.setMemberId(context.getMemberId());
    entity.setStartDate(LocalDate.parse(context.getStartDate()));
    entity.setEndDate(LocalDate.parse(context.getEndDate()));
    entity.setTrendJson(mapper.writeValueAsString(response.getTrendJson()));
    entity.setInsightJson(mapper.writeValueAsString(response.getInsightJson()));
  
    trendInsightDao.insertTrendInsight(entity);
  
    return response;
  }

  // DB entity -> DTO 변환
  private TrendAnalyzeResponse entityToResponse(TrendInsight entity) throws Exception {
    TrendAnalyzeResponse response = new TrendAnalyzeResponse();
    TrendGraph graph = mapper.readValue(entity.getTrendJson(), TrendGraph.class);
    InsightJson insight = mapper.readValue(entity.getInsightJson(), InsightJson.class);

    response.setTrendJson(graph);
    response.setInsightJson(insight);

    return response;
  }
}
