package com.example.demo.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PortfolioStandard {
    private Integer standardId;      // 평가 기준 ID
    private String standardName;     // 평가 항목명
    private String standardDescription;  // 평가 기준 설명
    private String promptTemplate;   // AI 프롬프트 템플릿
    private Integer weightPercentage = 20; 
    private String jobGroup;             // 직군 (개발, 디자인, 기획 등)
    private String jobRole;              // 직무 (백엔드, 프론트엔드, UX/UI 등)
    private String evaluationItems;      // 평가 항목 상세
    private String scoreRangeDescription; // 점수 범위 설명
}
