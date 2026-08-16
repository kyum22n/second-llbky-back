package com.example.demo.newstrend.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.example.demo.newstrend.service.NewsSummaryService;
import com.example.demo.newstrend.service.TotalNewsService;

import lombok.extern.slf4j.Slf4j;


/**
 * 뉴스 트렌드 컨트롤러
 * - 뉴스 분석
 * - 저장된 뉴스 조회
 */
@RestController
@RequestMapping("/trend/news")
@Slf4j
public class NewsController {

    @Autowired
    private TotalNewsService totalNewsService;

    @Autowired
    private NewsSummaryService newsSummaryService;

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayNews(
            @RequestParam int memberId,
            @RequestParam(defaultValue = "50") int limit) throws Exception {
        log.info("오늘 뉴스 조회 요청 - memberId: {}, limit: {}", memberId, limit);
        try {
            List<NewsAnalysisResponse> todayNewsList = totalNewsService.getTodayNewsByMember(memberId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "오늘 뉴스 조회 완료");
            response.put("data", todayNewsList);
            response.put("totalCount", todayNewsList.size());

            log.info("오늘 뉴스 조회 완료 - {}건 반환", todayNewsList.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("오늘 뉴스 조회 실패 - memberId: {}", memberId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "오늘 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("data", List.of());
            errorResponse.put("totalCount", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 회원 맞춤 뉴스 피드 조회

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getNewsFeed(
            @RequestParam int memberId,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastPublishedAt, // ✅
                                                                                                                               // 추가
            @RequestParam(required = false) Integer lastSummaryId,
            @RequestParam(defaultValue = "15") int limit) throws Exception {

        List<NewsAnalysisResponse> feedList = totalNewsService.getNewsFeed(
                memberId,
                period,
                lastPublishedAt, // ✅ 전달
                lastSummaryId, // ✅ 전달
                limit);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "뉴스 피드 조회 완료");
        response.put("data", feedList);
        response.put("totalCount", feedList.size());

        log.info("뉴스 피드 조회 완료 - {}건 반환", feedList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 네이버 뉴스 검색 및 수집
    
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchNews(
            @RequestParam(name = "keywords", required = false) String keywords,
            @RequestParam(name = "memberId", required = false) Integer memberId,
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "15") int limit

    ) {

        log.info("네이버 뉴스 검색 요청 - 키워드: {}, memberId: {}", keywords, memberId);

        if (keywords == null || keywords.isEmpty()) {
            Map<String, Object> bad = new HashMap<>();
            bad.put("status", "error");
            bad.put("message", "쿼리 파라미터 'keywords'를 하나 이상 전달해야 합니다.");
            bad.put("data", List.of()); // ✅ 빈 배열
            bad.put("totalCount", 0); // ✅ 추가
            bad.put("analyzed", 0);
            return ResponseEntity.badRequest().body(bad);
        }

        try {
            // String을 List로 변환
            List<String> keywordList = Arrays.asList(keywords.split(","));

            // 1. 뉴스 수집 및 분석
            int analyzed = totalNewsService.searchNews(keywordList, memberId, limit);

            // 2. 수집된 뉴스 조회
            List<NewsAnalysisResponse> newsList = newsSummaryService.searchNewsByUserKeywords(
                    keywordList,
                    period,
                    limit);

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("message", "뉴스 검색/수집 완료");
            resp.put("data", newsList);
            resp.put("totalCount", newsList.size());
            resp.put("analyzed", analyzed); // 추가 정보

            log.info("네이버 뉴스 검색 완료 - analyzed: {}, 조회: {}건", analyzed, newsList.size());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("뉴스 검색 실패 - keywords: {}, memberId: {}", keywords, memberId, e);

            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "뉴스 수집 중 오류가 발생했습니다: " + e.getMessage());
            err.put("data", List.of());
            err.put("totalCount", 0);
            err.put("analyzed", 0);

            return ResponseEntity.status(500).body(err);
        }
    }

    /**
     * 단일 뉴스 분석 및 저장
     * 
     * POST /news/analyze
     * 
     * 요청 예시:
     * {
     * "title": "AI 기술 발전으로 일자리 시장 변화",
     * "content": "인공지능 기술의 발전으로...",
     * "sourceUrl": "https://example.com/news/123",
     * "sourceName": "테크뉴스"
     * }
     * 
     * @param request 뉴스 분석 요청 (제목, 본문, URL, 출처)
     * @return 분석된 뉴스 응답 (요약, 감정, 신뢰도, 키워드 등)
     * @throws Exception AI 호출 실패 또는 JSON 변환 실패 시
     */
    @PostMapping("/analyze")
    public NewsAnalysisResponse analyzeNews(@RequestBody NewsAnalysisRequest request)
            throws Exception {
        log.info("뉴스 분석 요청 - 제목: {}", request.getTitle());

        NewsAnalysisResponse response = totalNewsService.analyzeAndSaveNews(request);

        log.info("뉴스 분석 완료 - summaryId: {}, 감정: {}",
                response.getSummaryId(),
                response.getSentiment());

        return response;
    }

    /**
     * 뉴스 상세보기
     * 
     * GET /trend/news/detail/{summaryId}
     * 
     * @param summaryId 뉴스 요약 ID
     * @return 뉴스 상세 분석 정보
     */
    @GetMapping("/detail/{summaryId}")
    public ResponseEntity<Map<String, Object>> getNewsDetail(@PathVariable int summaryId) {

        log.info("뉴스 상세보기 요청 - summaryId: {}", summaryId);

        try {
            NewsAnalysisResponse news = newsSummaryService.getNewsBySummaryId(summaryId);

            if (news == null) {
                log.warn("존재하지 않는 뉴스 ID - summaryId: {}", summaryId);
                Map<String, Object> notFound = new HashMap<>();
                notFound.put("status", "error");
                notFound.put("message", "해당 ID의 뉴스가 존재하지 않습니다.");
                notFound.put("data", null);
                return ResponseEntity.status(404).body(notFound);
            }

            // 상세 정보를 위한 데이터 재구성 (표시 순서대로 정리)
            Map<String, Object> detailData = new HashMap<>();
            detailData.put("summaryId", news.getSummaryId());
            detailData.put("sentiment", news.getSentiment());
            detailData.put("sentimentScores", news.getSentimentScores());
            
            detailData.put("title", news.getTitle());
            detailData.put("sourceName", news.getSourceName());
            detailData.put("sourceUrl", news.getSourceUrl());
            detailData.put("publishedAt", news.getPublishedAt());
            detailData.put("detailSummary", news.getDetailSummary());
            detailData.put("summaryText", news.getSummaryText());
            detailData.put("keywords", news.getKeywords());
            detailData.put("biasDetected", news.getBiasDetected());
            detailData.put("biasType", news.getBiasType());
            detailData.put("category", news.getCategory());
            detailData.put("createdAt", news.getCreatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "뉴스 상세 정보 조회 완료");
            response.put("data", detailData);

            log.info("뉴스 상세 조회 완료 - summaryId: {}, 제목: {}", summaryId, news.getTitle());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("뉴스 상세보기 실패 - summaryId: {}", summaryId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "뉴스 상세 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("data", null);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 관련뉴스 네이버 뉴스 검색 (외부 API 호출)
     * 
     * GET /trend/news/{summaryId}/related-search?limit=5
     * 
     * @param summaryId 현재 뉴스 ID
     * @param limit     각 키워드당 조회 개수 (기본값: 5)
     * @return 네이버 API에서 검색한 관련 뉴스 리스트
     */
    @GetMapping("/{summaryId}/related-search")
    public ResponseEntity<Map<String, Object>> searchRelatedNews(
            @PathVariable int summaryId,
            @RequestParam(defaultValue = "5") int limit) {

        log.info("키워드 기반 관련 뉴스 검색 - summaryId: {}, limit: {}", summaryId, limit);

        try {
            // 1. 현재 뉴스 조회해서 키워드 추출
            NewsAnalysisResponse currentNews = newsSummaryService.getNewsBySummaryId(summaryId);

            if (currentNews == null) {
                Map<String, Object> notFound = new HashMap<>();
                notFound.put("status", "error");
                notFound.put("message", "해당 뉴스가 존재하지 않습니다.");
                notFound.put("data", List.of());
                return ResponseEntity.status(404).body(notFound);
            }

            if (currentNews.getKeywords() == null || currentNews.getKeywords().isEmpty()) {
                Map<String, Object> noKeywords = new HashMap<>();
                noKeywords.put("status", "error");
                noKeywords.put("message", "해당 뉴스에 키워드가 없습니다.");
                noKeywords.put("data", List.of());
                return ResponseEntity.badRequest().body(noKeywords);
            }

            // ✅ List<NewsKeywordResponse>를 List<String>으로 변환
            List<String> keywordStrings = currentNews.getKeywords().stream()
                    .map(NewsKeywordResponse::getKeyword)
                    .collect(Collectors.toList());

            // 2. 키워드로 네이버 뉴스 API 검색
            List<Map<String, String>> relatedArticles = totalNewsService.searchNewsByKeywords(
                    keywordStrings, // ✅ 변환된 List<String> 전달
                    limit);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "관련 뉴스 검색 완료");
            response.put("keywords", keywordStrings); // ✅ String 리스트로 반환
            response.put("data", relatedArticles);
            response.put("totalCount", relatedArticles.size());

            log.info("관련 뉴스 검색 완료 - {}건 반환", relatedArticles.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("관련 뉴스 검색 실패 - summaryId: {}", summaryId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "관련 뉴스 검색 중 오류: " + e.getMessage());
            errorResponse.put("data", List.of());
            errorResponse.put("totalCount", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
