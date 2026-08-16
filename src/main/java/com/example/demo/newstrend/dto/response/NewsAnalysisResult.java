package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/*
AI가 생성한 전체 분석 작업 결과를 한 번에 담는 DTO

네이버 뉴스 원문을 AI에 넘기고

요약/감정/편향 분석 받고

키워드 추출하고

중립화된 최종 요약 만들고

이 모든 중간 결과를 묶어서 전달
*/


@Data
@Builder
public class NewsAnalysisResult {
    private String originalContent;        // 웹스크래핑된 원문
    private NewsSummaryResponse analysis;  // AI 분석 결과
    private List<NewsKeywordResponse> keywords; // 추출된 키워드
    private String finalSummary;// 최종 요약 (중립화 적용됨)

}
