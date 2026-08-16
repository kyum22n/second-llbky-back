package com.example.demo.newstrend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.newstrend.JobKeywordGenerationAgent;
import com.example.demo.ai.newstrend.JobRelevanceAgent;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsAnalysisResult;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * 뉴스 통합 서비스
 * - 각 서비스들을 조합하여 비즈니스 로직 처리
 * - NewsCollectorService: 수집
 * - NewsAIService: AI 분석
 * - NewsSummaryService: 저장
 * 
 * 단계 내용 클래스
 * 1번 뉴스 원문 수집 String(originalContent)
 * 2번 AI 분석 결과 조각들 반환 NewsSummaryResponse, NewsKeywordResponse
 * 3번 AI 결과를 하나로 묶음 NewsAnalysisResult ← 여기!
 * 4번 DB 저장용 entity로 변환 NewsSummary
 * 5번 프론트 응답 DTO NewsAnalysisResponse
 * 
 */
@Service
@Slf4j
public class TotalNewsService {
  @Autowired
  private JobKeywordGenerationAgent jobKeywordGenerationAgent; // ai로 직군별 키워드 생성

  @Autowired
  private JobRelevanceAgent jobRelevanceAgent;// 뉴스 - 직군 관련성 평가

  @Autowired
  private NewsAIService newsAIService; // 뉴스 AI 분석 (요약, 감정, 키워드)

  @Autowired
  private NewsCollectorService newsCollectorService; // 네이버 API로 뉴스 수집

  @Autowired
  private NewsSummaryService newsSummaryService;// DB 저장/조회

  @Autowired
  private ObjectMapper objectMapper; // JSON 직렬화/역직렬화

  @Autowired
  private MemberDao memberDao;// 회원 정보 조회

  TotalNewsService(JobKeywordGenerationAgent jobKeywordGenerationAgent) {
    this.jobKeywordGenerationAgent = jobKeywordGenerationAgent;
  }

  // 회원 맞춤 뉴스 피드 조회(오늘 데이터 없으면 자동 수집)
  @Transactional
  public List<NewsAnalysisResponse> getNewsFeed(
      int memberId, // 회원 ID - 개인화된 피드 제공용
      String period, // 조회 기간 ("week", "month" 등)
      LocalDateTime lastPublishedAt, // 무한 스크롤용 - 마지막 뉴스 발행일
      Integer lastSummaryId, // 무한 스크롤용 - 마지막 뉴스 ID
      int limit // 한 번에 가져올 뉴스 개수
  ) throws Exception {

    List<String> jobGroupKeywords = generateJobGroupKeywords(memberId);

    if (jobGroupKeywords.isEmpty()) {
      log.warn("키워드 생성 실패 - memberId: {}", memberId);
      return new ArrayList<>();
    }
    log.info("생성된 키워드: {}", jobGroupKeywords);

    List<NewsAnalysisResponse> feedList = newsSummaryService.getNewsByJobGroup(
        jobGroupKeywords,
        memberId,
        period,
        lastPublishedAt,
        lastSummaryId,
        limit);

    // ✅ 무한 스크롤 요청인지 확인
    boolean isInfiniteScroll = (lastPublishedAt != null || lastSummaryId != null);

    // 오늘 수집한 뉴스 존재 여부 확인
    boolean alreadyCollectedToday = newsSummaryService.existsTodayNews(memberId);

    // 피드 비어있을 때 처리
    if ((feedList == null || feedList.isEmpty())) {

        if (isInfiniteScroll) {
            return new ArrayList<>();
        }

        if (!alreadyCollectedToday) {
            log.info("피드 없음 + 오늘 미수집 → 수집");
            collectAndAnalyzeNews(jobGroupKeywords, memberId, limit);

            feedList = newsSummaryService.getNewsByJobGroup(
                jobGroupKeywords, memberId, period, null, null, limit
            );
        }
    }


  //   if (feedList == null || feedList.isEmpty()) {
  //     // 무한 스크롤이면 그냥 빈 배열 반환 (더 이상 없음)
  //     if (isInfiniteScroll) {
  //       log.info("무한 스크롤 - 추가 데이터 없음");
  //       return new ArrayList<>();
  //     }

  //     // 초기 로딩이고 데이터 없으면 자동 수집
  //     log.info("초기 피드 데이터 없음 - 자동 수집 시작");
  //     int analyzed = collectAndAnalyzeNews(jobGroupKeywords, memberId, limit);
  //     log.info("자동 수집 완료 - {}건 분석됨", analyzed);

  //     // 수집 후 다시 조회
  //     feedList = newsSummaryService.getNewsByJobGroup(
  //         jobGroupKeywords,
  //         memberId,
  //         period,
  //         null,
  //         null,
  //         limit);
  //   }

    log.info("뉴스 피드 조회 완료 - {}건", feedList != null ? feedList.size() : 0);
    return feedList != null ? feedList : new ArrayList<>();
  }

  /**
   * ✅ 사용자 직군에 맞는 키워드 생성
   */
  private List<String> generateJobGroupKeywords(int memberId) {
    // 1. 회원 정보 조회
    Member member = memberDao.findById(memberId);
    if (member == null) {
      return List.of("채용", "취업", "일자리");
    }

    List<String> baseKeywords = getBaseKeywordsByJobGroup(member.getJobGroup());

    List<String> aiKeywords = jobKeywordGenerationAgent.generateJobKeywords
    (member.getJobGroup(),
     member.getJobRole());

     // ✅ 1단계: AI 키워드 필터링 (15자 이하만)
    List<String> filteredAiKeywords = aiKeywords.stream()
        .filter(k -> k.length() <= 15)
        .collect(Collectors.toList());

    // ✅ 2단계: 모든 키워드 토큰화
    Set<String> tokenizedKeywords = new LinkedHashSet<>();
    
    // 기본 키워드 토큰화
    baseKeywords.forEach(keyword -> {
        Arrays.stream(keyword.split("\\s+"))
            .filter(token -> !token.isEmpty())
            .forEach(tokenizedKeywords::add);
    });
    
    // AI 키워드 토큰화
    filteredAiKeywords.forEach(keyword -> {
        Arrays.stream(keyword.split("\\s+"))
            .filter(token -> !token.isEmpty())
            .forEach(tokenizedKeywords::add);
    });

    List<String> result = new ArrayList<>(tokenizedKeywords);
    log.info("최종 토큰화 키워드: 기본{}개 + 필터AI{}개 → 총{}개 토큰 생성", 
        baseKeywords.size(), filteredAiKeywords.size(), result.size());
    
    return result;

  }

  private List<String> getBaseKeywordsByJobGroup(String jobGroup) {
    Map<String, List<String>> coreKeywords = Map.of(
        "마케팅", Arrays.asList("마케팅", "브랜드", "광고", "홍보", "캠페인"),
        "개발", Arrays.asList("개발자", "프로그래밍", "IT", "소프트웨어"),
        "디자인", Arrays.asList("디자인", "UI", "UX", "디자이너"),
        "기획", Arrays.asList("기획", "전략", "사업", "서비스"),
        "PM", Arrays.asList("PM", "매니저", "프로젝트", "관리"),
        "AI/데이터", Arrays.asList("AI", "데이터", "분석", "인공지능"),
        "영업", Arrays.asList("영업", "세일즈", "Sales"),
        "경영", Arrays.asList("경영", "관리", "전략"),
        "교육", Arrays.asList("교육", "강의", "트레이닝"),
        "기타", Arrays.asList("채용", "취업", "인사"));

    return coreKeywords.getOrDefault(jobGroup, Arrays.asList("채용", "취업", "일자리"));
  }

  /**
   * 단일 뉴스 분석 및 저장
   */
  @Transactional
  public NewsAnalysisResponse analyzeAndSaveNews(NewsAnalysisRequest request) throws Exception {
    // 중복 체크
    if (newsSummaryService.existsByUrl(request.getSourceUrl())) {
      log.warn("이미 존재하는 뉴스입니다.:{}", request.getSourceUrl());
      return null;
    }

    // AI 분석 수행
    NewsAnalysisResult analysisResult = newsAIService.analyzeNews(request);
    log.info("AI 분석 완료 - 감정: {}",
        analysisResult.getAnalysis().getSentiment());

    // 엔티티 생성 (NewsAnalysisResult 구조에 맞게 매핑) DTO → Entity 변환: API 응답을 DB 저장용 엔티티로 매핑
    NewsSummary entity = new NewsSummary();
    // 요청에 포함된 memberId 사용
    entity.setMemberId(request.getMemberId());
    entity.setTitle(request.getTitle());
    entity.setSourceName(request.getSourceName());
    entity.setSourceUrl(request.getSourceUrl());
    entity.setPublishedAt(LocalDateTime.now());
    // 최종 요약(중립화 적용된 문자열)
    entity.setSummaryText(analysisResult.getFinalSummary());
    // 상세 요약은 AI 분석 결과의 detailSummary 사용
    if (analysisResult.getAnalysis() != null) {
      entity.setDetailSummary(analysisResult.getAnalysis().getDetailSummary());
    }
    // analysisJson / keywordsJson은 ObjectMapper로 직렬화 복잡한 AI 분석 결과를 JSON으로 저장 (스키마
    // 유연성)
    entity.setAnalysisJson(objectMapper.writeValueAsString(analysisResult.getAnalysis()));
    entity.setKeywordsJson(objectMapper.writeValueAsString(analysisResult.getKeywords()));

    // 저장
    newsSummaryService.saveNewsSummary(entity);

    // Response 변환 및 반환
    NewsAnalysisResponse response = createResponse(entity, analysisResult);
    return response;
  }

  /**
   * 뉴스 수집, AI 분석 및 저장 통합 처리
   * - JobRelevanceAgent로 관련성 필터링 추가
   * 
   * @param keywords 검색 키워드 목록
   * @param memberId 회원 ID
   * @param limit    수집 제한 개수
   * @return 분석된 뉴스 개수
   */
  @Transactional // 트랜잭션 처리: 전체 작업이 성공/실패 단위로 처리됨
  public int collectAndAnalyzeNews(List<String> keywords, Integer memberId, int limit) throws Exception {
    // INFO 로그: 처리 시작 시점과 주요 파라미터 기록
    log.info("뉴스 수집 및 분석 통합 처리 시작 - keywords: {}, memberId: {}", keywords, memberId);

    // ========== 1단계: 뉴스 수집 ==========
    // NewsCollectorService를 통해 네이버 API에서 뉴스 수집
    List<NewsAnalysisRequest> collectedNews = newsCollectorService.collectNews(keywords, memberId, limit);
    log.info("뉴스 수집 완료 - {}건", collectedNews.size()); // 수집된 뉴스 개수 로그

    // 수집된 뉴스가 없으면 조기 종료
    if (collectedNews.isEmpty()) {
      log.info("수집된 뉴스가 없습니다.");
      return 0; // 분석된 개수 0 반환
    }

    // ========== 2단계: 회원의 직군 정보 조회 (관련성 평가용) ==========
    Member member = memberDao.findById(memberId); // DB에서 회원 정보 조회
    // 직군이 없으면 "기타"로 기본값 설정 (null 안전 처리)
    String jobGroup = (member != null && member.getJobGroup() != null) ? member.getJobGroup() : "기타";

    // ========== 3단계: JobRelevanceAgent로 관련성 필터링 ==========
    List<NewsAnalysisRequest> relevantNews = new ArrayList<>(); // 관련성 높은 뉴스 저장용 리스트
    int filteredCount = 0; // 필터링된 뉴스 개수 카운터

    // 수집된 모든 뉴스에 대해 관련성 평가
    for (NewsAnalysisRequest news : collectedNews) {
     
        // AI Agent를 통해 뉴스-직군 간 관련성 점수 계산 (0-100점)
        int relevanceScore = jobRelevanceAgent.calculateRelevanceScore(news, jobGroup);

        // 관련성 점수가 15점 이상인 뉴스만 선별 (임계값)
        if (relevanceScore >= 15) {
          relevantNews.add(news); // 관련성 높은 뉴스 리스트에 추가
          // log.debug("관련성 높은 뉴스 선택: {} (점수: {})", news.getTitle(), relevanceScore);
        } else {
          filteredCount++; // 필터링된 뉴스 카운트 증가
          // log.debug("관련성 낮은 뉴스 필터링: {} (점수: {})", news.getTitle(), relevanceScore);
        }

        // API 호출 제한(Rate Limit) 방지를 위한 짧은 대기
        Thread.sleep(200); // 200ms 대기 (초당 최대 5회 호출)

     
    }

    // 필터링 결과 요약 로그
    log.info("관련성 필터링 완료 - 전체: {}건, 관련성 높음: {}건, 필터링: {}건",
        collectedNews.size(), relevantNews.size(), filteredCount);

    // 관련성 높은 뉴스가 없으면 조기 종료
    if (relevantNews.isEmpty()) {
      log.info("관련성 높은 뉴스가 없습니다.");
      return 0; // 분석된 개수 0 반환
    }

    // ========== 4단계: 관련성 높은 뉴스만 AI 분석 및 저장 ==========
    int totalAnalyzed = 0; // 성공적으로 분석된 뉴스 개수
    int duplicateCount = 0; // 중복된 뉴스 개수
    int errorCount = 0; // 에러 발생 개수

    // 관련성 높은 뉴스에 대해서만 AI 분석 수행 (비용/시간 절약)
    for (NewsAnalysisRequest newsRequest : relevantNews) {
      
        // 4-1. AI 분석 실행 (NewsAIService)
        log.debug("AI 분석 시작: {}", newsRequest.getTitle());
        NewsAnalysisResult analysisResult = newsAIService.analyzeNews(newsRequest);

        // 4-2. 엔티티 생성 및 AI 분석 결과 매핑
        NewsSummary entity = new NewsSummary(); // DB 저장용 엔티티 생성
        entity.setMemberId(newsRequest.getMemberId()); // 회원 ID
        entity.setTitle(newsRequest.getTitle()); // 뉴스 제목
        entity.setSourceName(newsRequest.getSourceName()); // 출처 (네이버 등)
        entity.setSourceUrl(newsRequest.getSourceUrl()); // 원문 URL
        // 발행일: 수집된 날짜가 있으면 사용, 없으면 현재 시각
        entity.setPublishedAt(
            newsRequest.getPublishedAt() != null
                ? newsRequest.getPublishedAt()
                : LocalDateTime.now());

        // 4-3. AI 분석 결과를 JSON으로 직렬화하여 저장
        entity.setSummaryText(analysisResult.getFinalSummary()); // AI 요약문
        entity.setDetailSummary(analysisResult.getAnalysis().getDetailSummary()); // 상세 요약
        // ObjectMapper로 Java 객체 → JSON 문자열 변환
        entity.setAnalysisJson(objectMapper.writeValueAsString(analysisResult.getAnalysis())); // 감정/신뢰도 등
        entity.setKeywordsJson(objectMapper.writeValueAsString(analysisResult.getKeywords())); // 키워드 리스트

        // 4-4. DB에 저장
        NewsSummary savedSummary = newsSummaryService.saveNewsSummary(entity);
        if (savedSummary == entity) {
          totalAnalyzed++;
          log.info("뉴스 분석 및 저장 완료: {} (감정: {})",
              newsRequest.getTitle(),
              analysisResult.getAnalysis().getSentiment());
        } else {
          duplicateCount++;
          log.debug("중복 뉴스 스킵: {}", newsRequest.getTitle());
        }

        // API 호출 제한(Rate Limit) 방지를 위한 대기
        Thread.sleep(2000); // 1초 대기 (AI 분석은 시간이 걸리므로 여유있게 설정)

    }

    // ========== 최종 결과 요약 로그 ==========
    log.info("뉴스 수집 및 분석 완료 - 수집: {}건, 관련성 필터: {}건, 분석 성공: {}건, 중복: {}건, 오류: {}건",
        collectedNews.size(), // 최초 수집된 뉴스 개수
        relevantNews.size(), // 관련성 필터링 통과한 뉴스 개수
        totalAnalyzed, // 최종 분석 성공한 뉴스 개수
        duplicateCount, // 중복 제거된 개수
        errorCount); // 에러 발생 개수

    return totalAnalyzed; // 성공적으로 분석된 뉴스 개수 반환
  }

  /**
   * 오늘 날짜 뉴스 조회(없으면 자동 수집)
   */
  @Transactional
  public List<NewsAnalysisResponse> getTodayNewsByMember(int memberId, int limit) throws Exception {
    log.info("오늘 뉴스 조회 - memberId: {}, limit: {}", memberId, limit);

    List<String> jobGroupKeywords = generateJobGroupKeywords(memberId);

    // 1단계: 오늘 뉴스 체크 (기존 메서드 활용)
    // List<NewsAnalysisResponse> todayNews = newsSummaryService.getTodayNewsByMember(memberId, 15);

    // // 2단계 데이터 없으면 자동 수집
    // if (todayNews == null || todayNews.size() < 5) {
    //   log.info("오늘 뉴스 데이터 없음 - 자동 수집 시작");

    //   int analyzed = collectAndAnalyzeNews(jobGroupKeywords, memberId, limit); // ✅ 통합 메소드 호출

    //   log.info("자동 수집 완료 - {}건 분석됨", analyzed);
    // } else {
    //   log.info("오늘 뉴스 이미 존재 - {}건", todayNews.size());
    // }

    // 오늘 뉴스 존재 여부 확인
    boolean alreadyCollectedToday = newsSummaryService.existsTodayNews(memberId);

    // 오늘 뉴스 없으면 자동 수집
    if (!alreadyCollectedToday) {
        log.info("오늘 뉴스 없음 → 자동 수집");
        collectAndAnalyzeNews(jobGroupKeywords, memberId, limit);
    } else {
        log.info("오늘 뉴스 이미 수집됨");
    }


    // 3단계: 일주일치 뉴스 조회

    List<NewsAnalysisResponse> weeklyNews = newsSummaryService.getNewsByJobGroup(
        jobGroupKeywords, memberId, "week", null, null, limit);

    if (weeklyNews == null || weeklyNews.size() < 20) { // ✅ 최소 20개 확보
      log.info("데이터 부족({} 건) - 대량 수집 시작", weeklyNews != null ? weeklyNews.size() : 0);

      // 대량 수집 (100개 요청)
      int analyzed = collectAndAnalyzeNews(jobGroupKeywords, memberId, 50); // ✅ 100개
      log.info("대량 수집 완료 - {}건 분석됨", analyzed);

      // 수집 후 다시 조회
      weeklyNews = newsSummaryService.getNewsByJobGroup(
          jobGroupKeywords, memberId, "week", null, null, limit);
    } else {
      log.info("충분한 데이터 존재 - {}건", weeklyNews.size());
    }

    log.info("일주일치 뉴스 반환 - {}건", weeklyNews != null ? weeklyNews.size() : 0);
    return weeklyNews != null ? weeklyNews : new ArrayList<>();
  }

  

  /**
   * 뉴스 검색 및 수집 (API 엔드포인트용)
   */
  @Transactional
  public int searchNews(List<String> keywords, Integer memberId, int limit) throws Exception {
    log.info("뉴스 검색 시작 - keywords: {}, memberId: {}", keywords, memberId);

    // ✅ 통합 메소드 호출
    int analyzed = collectAndAnalyzeNews(keywords, memberId, limit);

    log.info("뉴스 검색 완료 - {}건 분석됨", analyzed);
    return analyzed;
  }

  /**
   * 키워드 리스트로 네이버 뉴스 검색 (단순 검색, 저장 안함)
   * 
   * @param keywords        검색할 키워드 리스트
   * @param limitPerKeyword 각 키워드당 가져올 기사 수
   * @return 기사 제목, URL, 설명이 담긴 리스트
   */
  public List<Map<String, String>> searchNewsByKeywords(List<String> keywords, int limitPerKeyword) {
    return newsCollectorService.searchNewsByKeywords(keywords, limitPerKeyword);
  }

  /**
   * Response 생성 헬퍼 메서드 // Entity → DTO 변환 로직
   */
  private NewsAnalysisResponse createResponse(NewsSummary saved, NewsAnalysisResult result) {
    NewsAnalysisResponse response = new NewsAnalysisResponse();

    // 기본 정보
    response.setSummaryId(saved.getSummaryId());
    response.setTitle(saved.getTitle());
    response.setSourceName(saved.getSourceName());
    response.setSourceUrl(saved.getSourceUrl());
    response.setPublishedAt(saved.getPublishedAt());

    // 요약 정보
    response.setSummaryText(saved.getSummaryText());
    response.setDetailSummary(saved.getDetailSummary());

    // AI 분석 결과
    NewsSummaryResponse analysis = result.getAnalysis();
    response.setSentiment(analysis.getSentiment());
    response.setSentimentScores(analysis.getSentimentScores());

    response.setBiasDetected(analysis.getBiasDetected());
    response.setBiasType(analysis.getBiasType());
    response.setCategory(analysis.getCategory());

    // 키워드

    response.setKeywords(result.getKeywords());

    response.setCreatedAt(saved.getCreatedAt());

    return response;
  }

}
