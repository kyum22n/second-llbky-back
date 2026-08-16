package com.example.demo.newstrend.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.newstrend.GrowthAnalysisAgent;
import com.example.demo.ai.newstrend.JobRelatedInsightAgent;
import com.example.demo.ai.newstrend.NewsSecondSummaryAgent;
import com.example.demo.newstrend.dao.JobInsightDao;
import com.example.demo.newstrend.dto.response.GrowthAnalysisResponse;
import com.example.demo.newstrend.dto.response.JobInsightListResponse;
import com.example.demo.newstrend.entity.JobInsight;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobInsightService {
  @Autowired
  private JobRelatedInsightAgent jobRelatedInsightAgent;
  @Autowired
  private GrowthAnalysisAgent growthAnalysisAgent;
  @Autowired
  private JobInsightDao jobInsightDao;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private NewsSecondSummaryAgent newsSecondSummaryAgent;

  // 직무 인사이트 생성
  @Transactional
  public JobInsight createJobInsight(int memberId) throws Exception {
    // 기존 인사이트 모두 삭제
    JobInsight latest = jobInsightDao.selectLatestJobInsight(memberId);
    if(latest != null){
      jobInsightDao.deleteJobInsight(latest.getInsightId());
    }

    // 뉴스 2차 요약 에이전트 호출
    String metaNews = newsSecondSummaryAgent.summarizeNews(memberId, 50);

    // 성장 제안 생성 에이전트 호출
    GrowthAnalysisResponse growthAdviceJson = growthAnalysisAgent.generateGrowthAdvice(memberId, metaNews);

    // 직무 인사이트 카드 생성하는 에이전트 호출
    JobInsightListResponse cards = jobRelatedInsightAgent.relatedJobs(memberId, metaNews);

    // DB 저장
    JobInsight entity = new JobInsight();
    entity.setMemberId(memberId);
    entity.setAnalysisJson(mapper.writeValueAsString(growthAdviceJson));
    entity.setRelatedJobsJson(mapper.writeValueAsString(cards));

    jobInsightDao.insertJobInsight(entity);

    return entity;
  }

  // 최근 인사이트 1개 조회
  public JobInsight getLatestInsight(int memberId) {
    return jobInsightDao.selectLatestJobInsight(memberId);
  }

  // 조회 후 없으면 + 7일마다 재생성
  public JobInsight getOrCreateInsight(int memberId) throws Exception {
    JobInsight latest = jobInsightDao.selectLatestJobInsight(memberId);
    if (latest == null) {
      return createJobInsight(memberId);
    }

    // 날짜 체크: created_at 기준 7일 지났으면 갱신
    LocalDate createdDate = latest.getCreatedAt().toLocalDate();
    LocalDate today = LocalDate.now();

    if (createdDate.plusDays(7).isBefore(today)) {
      // 새로 갱신
      return createJobInsight(memberId);
    }

    return latest;
  }

  // 키워드 저장 / 삭제시 성장 제안 수정
  public JobInsight modifyGrowthAnalysis(int memberId) throws Exception {
    // 최신 인사이트 조회
    JobInsight latest = getLatestInsight(memberId);
    if (latest == null) {
      return createJobInsight(memberId);
    }

    // 뉴스 2차 요약 에이전트 호출
    String metaNews = newsSecondSummaryAgent.summarizeNews(memberId, 10);

    // 성장 제안 생성 에이전트 호출
    GrowthAnalysisResponse growthAdviceJson = growthAnalysisAgent.generateGrowthAdvice(memberId, metaNews);

    // DB 저장
    latest.setAnalysisJson(mapper.writeValueAsString(growthAdviceJson));
    jobInsightDao.updateAnalysisJson(latest);

    return latest;

  }

}
