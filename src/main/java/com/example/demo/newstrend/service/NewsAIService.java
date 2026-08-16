package com.example.demo.newstrend.service;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.newstrend.BiasNeutralizationAgent;
import com.example.demo.ai.newstrend.KeywordExtractionAgent;
import com.example.demo.ai.newstrend.NewsAnalysisAgent;
import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResult;
import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 통합 서비스
 * - AI Agent들을 순서대로 실행하여 뉴스 분석
 * - 기사 원문 저장, 요약문 저장까지 담당
 * - 흐름: Controller → Service (AI Agent로 데이터 가공) → 저장 → DB
 */
@Service
@Slf4j
public class NewsAIService {

    // AI Agents
    @Autowired
    private NewsAnalysisAgent analysisAgent;

    @Autowired
    private KeywordExtractionAgent keywordAgent;

    @Autowired
    private BiasNeutralizationAgent neutralizationAgent;

    /**
     * 뉴스 분석  (AI Agent 활용)
     * @param request 뉴스 분석 요청 (제목, 본문, URL 등)
     * @return 분석된 뉴스 응답 (요약, 감정, 신뢰도, 키워드 등)
     * @throws Exception AI 호출 실패 또는 JSON 변환 실패 시
     */
   @Transactional
// 트랜잭션 범위 내에서 실행 → DB 저장이 포함될 경우 롤백 가능
    public NewsAnalysisResult analyzeNews(NewsAnalysisRequest request) throws Exception {
        log.info("뉴스 분석 및 저장 시작 - 제목: {}", request.getTitle());
        // 분석 시작 로그, 어떤 뉴스인지 확인용

        // 0. 원문 확보 (짧으면 웹 스크래핑)
        String fullContent = request.getContent();
        // 요청에 담긴 뉴스 본문(content)을 기본으로 사용

        if (fullContent == null || fullContent.length() < 200) {
            // 본문이 없거나 너무 짧으면 웹에서 본문 가져오기
            log.info("짧은 content 감지, 웹 스크래핑 시도: {}", request.getSourceUrl());
            // 스크래핑 시도 로그

            String scrapedContent = extractNewsContent(request.getSourceUrl());
            // extractNewsContent 메서드로 실제 기사 본문 가져오기

            if (scrapedContent != null && scrapedContent.length() > fullContent.length()) {
                fullContent = scrapedContent;
                // 가져온 본문이 기존보다 길면 fullContent를 업데이트
            }
        }

        // 2. AI Agent: 뉴스 분석 (요약, 감정, 신뢰도, 편향, 카테고리)
        NewsSummaryResponse analysis = analysisAgent.analyzeNews(request.getTitle(), fullContent);
        // 분석Agent 호출 → 제목+본문으로 AI 분석 → 요약, 감정, 편향, 신뢰도, 카테고리 반환

        // 3. AI Agent: 키워드 추출
        List<NewsKeywordResponse> keywords = keywordAgent.extractKeywords(analysis.getSummary());
        // 분석된 요약(summary)에서 키워드 추출

        // 4. AI Agent: 편향 감지시 중립화
        String finalSummary = analysis.getSummary();
        // 기본 finalSummary는 AI가 분석한 요약(summary)

        if (Boolean.TRUE.equals(analysis.getBiasDetected())) {
            // biasDetected가 true이면 편향 감지됨
            finalSummary = neutralizationAgent.neutralizeText(fullContent);
            // 중립화 처리 후 finalSummary 업데이트
        } else {
            log.debug("편향 미감지 - 중립화 스킵");
            // 편향이 없으면 중립화 건너뜀
        }

        // 5. 분석 결과 반환 (저장 안함)
        return NewsAnalysisResult.builder()
            .originalContent(fullContent)
            // 원문 본문
            .analysis(analysis)
            // AI 분석 결과 DTO
            .keywords(keywords)
            // 키워드 리스트 DTO
            .finalSummary(finalSummary)
            // 최종 요약(중립화 적용 가능)
            .build();
            // NewsAnalysisResult 객체 생성 및 반환
    }

    /**
     * 웹 스크래핑: 뉴스 본문 추출
     * - 짧은 본문이 들어왔을 때 원문을 가져오기 위해 사용
     * - User-Agent 설정으로 정상적인 HTML 반환 유도
     * @param url 뉴스 URL
     * @return 추출된 본문 (실패시 null)
     */
    private String extractNewsContent(String url) {
        log.info("웹 스크래핑 시작 - URL: {}", url);
        
        try {
            // SSL 인증서 검증 우회
            com.example.demo.config.SSLHelper.disableSSLVerification();
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .ignoreHttpErrors(true) // HTTP 에러 무시
                    .followRedirects(true)  // 리다이렉트 따라가기
                    .get();

            StringBuilder content = new StringBuilder();
            
            // 본문 추출 시도 1: article 태그
            Element articleBody = doc.selectFirst("article, .article-body, .news-body, .content, main");
            if (articleBody != null && !articleBody.text().isEmpty()) {
                content.append(articleBody.text());
            } else {
                // 본문 추출 시도 2: p 태그들
                for (Element p : doc.select("p")) {
                    String text = p.text();
                    if (text.length() > 50) { // 짧은 문장 제외
                        content.append(text).append(" ");
                    }
                }
            }

            String result = content.toString().trim();
            
            if (result.length() < 100) {
                log.warn("스크래핑된 내용이 너무 짧음 ({}자) - URL: {}", result.length(), url);
                return null;
            }
            
            if (result.length() > 5000) {
                result = result.substring(0, 5000);
            }
            
            log.info("웹 스크래핑 성공 - URL: {}, 내용 길이: {}", url, result.length());
            return result;
            
        } catch (Exception e) {
            log.error("웹 스크래핑 오류 - URL: {}, 오류: {}", url, e.getMessage());
            return null;
        }
    }

}