package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

// 산업별 뉴스 감정 분석 & 근거

@Data
public class SentimentResponse {
    private String industry;  // 예: "IT", "의료", "금융"

    private SentimentRatio sentimentRatio;  // 긍정/중립/부정 비율 (총합 100)

    private List<IssueAnalysis> issues;  // 이슈 분석 리스트

    private String overallSummary; // 전체 종합 요약
}
