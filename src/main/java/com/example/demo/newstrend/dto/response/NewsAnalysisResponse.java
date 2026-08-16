package com.example.demo.newstrend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

    /*
    DB에 저장된 데이터를 파싱해서 프론트로 보내는 최종 응답 DTO

    DB의 NewsSummary 엔티티 안에 있는

    summaryText

    detailSummary

    analysisJson → sentiment 등

    keywordsJson → List<NewsKeywordResponse>
    등을 변환하여 구성됨.
 */

@Data
public class NewsAnalysisResponse {
    private Integer summaryId;
    private String title;
    private String sourceName;
    private String sourceUrl;
    private LocalDateTime publishedAt;
    
    // 요약 정보
    private String summaryText;
    private String detailSummary;
    
    // AI 분석 결과
    private String sentiment;
    private SentimentScores sentimentScores;
    
    private Boolean biasDetected;
    private String biasType;
    private String category;
    
    // 키워드
    private List<NewsKeywordResponse> keywords; 
    
    private LocalDateTime createdAt;
    
    
}
