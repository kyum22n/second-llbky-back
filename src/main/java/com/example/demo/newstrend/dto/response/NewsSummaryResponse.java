package com.example.demo.newstrend.dto.response;



import lombok.Data;
/*
LLM(AI)에게 요약/감정/편향/신뢰도 분석을 요청했을 때
AI가 반환한 분석 데이터를 담는 DTO

DB 저장에는 사용되지 X → 서비스 내부 로직에서 사용됨
*/


@Data
public class NewsSummaryResponse {
    private String summary;
    private String detailSummary;  
    private String sentiment;
    private SentimentScores sentimentScores;
    
    private Boolean biasDetected;
    private String biasType;
    private String category;
    
}
