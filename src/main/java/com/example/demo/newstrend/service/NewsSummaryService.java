package com.example.demo.newstrend.service;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;
import com.example.demo.newstrend.dto.response.SentimentScores;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//중복 체크, 분석 응답 파싱용

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsSummaryService {

    private final NewsSummaryDao newsSummaryDao;
    private final ObjectMapper objectMapper;

    /**
     * 뉴스 저장
     * 
     * @param newsSummary 저장할 뉴스 엔티티
     * @return 저장된 뉴스 (중복일 경우 기존 데이터 반환)
     */
    @Transactional
    public NewsSummary saveNewsSummary(NewsSummary newsSummary) {
        log.info("뉴스 저장 시도 - 제목: {}, URL: {}",
                newsSummary.getTitle(),
                newsSummary.getSourceUrl());

        // URL 중복 체크
        NewsSummary existing = newsSummaryDao.selectNewsSummaryBySourceUrl(
                newsSummary.getSourceUrl());

        if (existing != null) {
            log.info("이미 존재하는 뉴스 URL: {}", newsSummary.getSourceUrl());
            return existing;
        }

        newsSummaryDao.insertNewsSummary(newsSummary);
        log.info("뉴스 저장 완료 - summaryId: {}", newsSummary.getSummaryId());

        return newsSummary;
    }

    // 1.오늘 날짜 뉴스 조회 없으면 자동수집
    public List<NewsAnalysisResponse> getTodayNewsByMember(int memberId, int limit) throws Exception {

        LocalDateTime today = LocalDateTime.now();
        List<NewsSummary> todayNews = newsSummaryDao.selectNewsByMemberAndDate(memberId, today, limit);

        // 2. 오늘 데이터가 있으면 DB값 반환
        if (todayNews != null && !todayNews.isEmpty()) {
            log.info("오늘 뉴스 데이터 존재 -{}건 반환", todayNews.size());
            List<NewsAnalysisResponse> responses = new ArrayList<>();
            for (NewsSummary summary : todayNews) {
                responses.add(convertToResponse(summary));
            }
            return responses;
        }
        // 데이터 없을때 빈값만 리턴함
        List<NewsAnalysisResponse> responses = new ArrayList<>();
        return responses;

    }

    /**
     * 검색창 검색용 - 사용자 입력 키워드만, memberId 무관
     * 
     * @throws JsonProcessingException
     */
    public List<NewsAnalysisResponse> searchNewsByUserKeywords(
            List<String> keywords,
            String period,
            int limit) throws JsonProcessingException {


        List<NewsSummary> summaries = newsSummaryDao.searchNewsByKeywordsAndDate(
                keywords,
                period,
                limit);

        List<NewsAnalysisResponse> responses = new ArrayList<>();
        for (NewsSummary summary : summaries) {
            responses.add(convertToResponse(summary));
        }

        return responses;
    }

    /**
     * 기본 피드용 - 직군 기반 여러 키워드 + memberId 필터링
     */
    public List<NewsAnalysisResponse> getNewsByJobGroup(
            List<String> jobGroupKeywords,
            Integer memberId,
            String period,
            LocalDateTime lastPublishedAt,
            Integer lastSummaryId,
            int limit) throws Exception {

        List<NewsSummary> summaries = newsSummaryDao.findByJobGroupKeywords(
            jobGroupKeywords, 
            memberId, 
            period,            // ✅ period 직접 전달
            lastPublishedAt,  
            lastSummaryId, 
            limit
        );

        List<NewsAnalysisResponse> responses = new ArrayList<>();
        for (NewsSummary summary : summaries) {
            responses.add(convertToResponse(summary));
        }

        return responses;
    }

    public boolean existsByUrl(String sourceUrl) {
        return newsSummaryDao.selectNewsSummaryBySourceUrl(sourceUrl) != null;
    }

    /**
     * 특정 회원의 특정 날짜 뉴스 조회
     * 
     * @param memberId 회원 ID
     * @param date     YYYY-MM-DD 형식의 날짜 문자열
     * @param limit    조회 개수
     * @return 뉴스 분석 결과 리스트
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     */
    public List<NewsAnalysisResponse> getNewsByMemberAndDate(int memberId, LocalDateTime date, int limit)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        log.info("회원별 날짜 뉴스 조회 - memberId: {}, date: {}, limit: {}", memberId, date, limit);

        List<NewsSummary> summaries = newsSummaryDao.selectNewsByMemberAndDate(memberId, date, limit);
        log.info("조회된 뉴스 수: {}", summaries != null ? summaries.size() : 0);

        List<NewsAnalysisResponse> responses = new ArrayList<>();
        if (summaries != null) {
            for (NewsSummary summary : summaries) {
                NewsAnalysisResponse response = convertToResponse(summary);
                responses.add(response);
            }
        }

        return responses;
    }
    // summaryId로 단일 뉴스 조회 (상세보기용)

    public NewsAnalysisResponse getNewsBySummaryId(int summaryId)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("단일 뉴스 조회 - summaryId: {}", summaryId);

        NewsSummary summary = newsSummaryDao.selectNewsSummaryById(summaryId);

        if (summary == null) {
            log.warn("존재하지 않는 뉴스 - summaryId: {}", summaryId);
            return null;
        }

        NewsAnalysisResponse response = convertToResponse(summary);
        log.info("단일 뉴스 조회 완료 - summaryId: {}", summaryId);

        return response;
    }

    /**
     * ✅ Entity -> Response 변환 (analysisMap 없이 DTO 직접 변환) - 버그 수정
     * 
     * @param summary 뉴스 엔티티
     * @return 뉴스 분석 응답 DTO
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     * 
     */
    public NewsAnalysisResponse convertToResponse(NewsSummary summary)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        // summary 엔티티(또는 DTO)를 받아서 API용 응답 DTO인 NewsAnalysisResponse로 변환하는 메서드
        // JSON 파싱 과정에서 Jackson의 JsonProcessingException이 발생할 수 있으므로 throws로 선언함

        // 기본값 설정
        NewsSummaryResponse defaultAnalysis = new NewsSummaryResponse();
        // 분석 결과가 없을 때 사용할 기본값 객체를 생성

        defaultAnalysis.setSentiment("중립");
        // 감성 분석 결과가 없을 경우 기본 감성은 "중립"으로 세팅
        SentimentScores defaultScores = new SentimentScores();
        defaultScores.setPositive(0);
        defaultScores.setNegative(0);
        defaultScores.setNeutral(100);
        defaultAnalysis.setSentimentScores(defaultScores);
        
        // 신뢰도 점수 기본값을 50으로 세팅(0~100 스케일을 가정)

        defaultAnalysis.setBiasDetected(false);
        // 편향 여부 기본값: 탐지되지 않음

        defaultAnalysis.setBiasType("없음");
        // 편향 유형 기본값: 없음

        defaultAnalysis.setCategory("일반");
        // 카테고리 기본값: 일반

        log.debug("Response 변환 시작 - summaryId: {}", summary.getSummaryId());
        // 디버그 로그: 변환 시작, 어떤 summaryId인지 기록 -> 문제 발생 시 추적 용도

        // ✅ 분석 데이터 변환
        NewsSummaryResponse analysisData;
        // DB에 저장된 analysisJson이 있으면 파싱해서 analysisData로 사용하고, 없으면 defaultAnalysis 사용

        if (summary.getAnalysisJson() != null && !summary.getAnalysisJson().trim().isEmpty()) {
            analysisData = objectMapper.readValue(summary.getAnalysisJson(), NewsSummaryResponse.class);
            // summary.getAnalysisJson() 문자열(JSON)을 Jackson의 ObjectMapper로
            // NewsSummaryResponse 객체로 역직렬화
            // 이 과정에서 Json 형식이 올바르지 않으면 JsonProcessingException이 발생할 수 있음
        } else {
            analysisData = defaultAnalysis;
            // analysisJson이 없거나 공백이면 앞에서 만든 기본값을 사용
        }

        // ✅ 키워드 데이터 변환 (별도 변수 사용)
        List<NewsKeywordResponse> keywords = new ArrayList<>();
        // 키워드 리스트 초기화 — 기본은 빈 리스트(널이 아닌 빈 리스트로 가져가는 것이 안전)

        if (summary.getKeywordsJson() != null && !summary.getKeywordsJson().trim().isEmpty()) {
            keywords = objectMapper.readValue(
                    summary.getKeywordsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NewsKeywordResponse.class));
            // keywordsJson이 존재하면 JSON 배열을 List<NewsKeywordResponse>로 역직렬화
            // getTypeFactory : 타입 팩토리: 주어진 삽입 값을 구체적인 자바타입형으로 만드는 클래스, 맞춤화 되지 않는 전역 인스턴스에
            // 접근하기 위한 메서드,
            // ObjectMapper에서 구성된 기본 객체를 얻을때 사용한다.
            // constructCollectionType(...)을 사용해 제네릭 타입(List<NewsKeywordResponse>)을 정확히 지정
            // -> Jackson이 제네릭 타입을 올바르게 처리하도록 도와줌
        }
        // keywords가 빈 리스트이든 파싱된 값이든, 이후 응답에 안전하게 넣을 수 있음

        // ✅ Response 객체 생성
        NewsAnalysisResponse response = new NewsAnalysisResponse();
        // 최종 반환할 응답 DTO 객체 생성

        // 기본 정보
        response.setSummaryId(summary.getSummaryId());
        // summary의 ID를 응답에 설정 (식별용)

        response.setTitle(summary.getTitle());
        // 원문 제목 설정

        response.setSourceName(summary.getSourceName());
        // 출처 이름 설정 (예: 네이버뉴스 등)

        response.setSourceUrl(summary.getSourceUrl());
        // 출처 URL 설정 (원문 링크)

        response.setPublishedAt(summary.getPublishedAt());
        // 기사 발행 시각(또는 게시일) 설정

        response.setSummaryText(summary.getSummaryText());
        // 짧은 요약 텍스트 설정

        response.setDetailSummary(summary.getDetailSummary());
        // 상세 요약(더 긴 요약) 설정

        // ✅ 분석 결과
        response.setSentiment(analysisData.getSentiment());
        // 파싱된(또는 기본) 분석 데이터에서 감성 결과를 꺼내서 응답에 넣음
        response.setSentimentScores(analysisData.getSentimentScores());

        response.setBiasDetected(analysisData.getBiasDetected());
        // 편향 탐지 여부 설정

        response.setBiasType(analysisData.getBiasType());
        // 편향 유형 설정

        response.setCategory(analysisData.getCategory());
        // 분석에서 나온 카테고리 설정

        // ✅ 키워드
        response.setKeywords(keywords);
        // 파싱된 키워드 리스트(또는 빈 리스트)를 응답에 넣음 — null이 아니어서 클라이언트 처리에 안전

        response.setCreatedAt(summary.getCreatedAt());
        // summary가 생성된 시간(또는 DB 저장 시간)을 응답에 포함

        log.debug("Response 변환 완료 - summaryId: {}", summary.getSummaryId());
        // 디버그 로그: 변환 완료

        return response;
        // 변환된 응답 DTO 반환
    }

    // 오늘 수집된 뉴스 존재 여부 확인
    public boolean existsTodayNews(int memberId) {
        return newsSummaryDao.existsTodayNews(memberId) > 0;
    }
}