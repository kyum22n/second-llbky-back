package com.example.demo.newstrend.entity;


import java.time.LocalDateTime;

import lombok.Data;

/*
  DB에서 관리하는 핵심 엔티티

  네이버 API에서 가져온 원문 → 요약 → AI 분석 → 키워드
  → 이 모든 결과를 한 엔티티에 저장(JSON 문자열 포함)

  이후 조회 시 DB에서 꺼내 JSON 파싱하여 DTO로 변환해 프론트에 전달.
*/


@Data
public class NewsSummary {
  private Integer summaryId; // 뉴스 요약 ID(PK)
  private Integer memberId; // 멤버 ID
  private String sourceName; // 언론사 이름
  private String sourceUrl; // 원문 뉴스 URL
  private String title; // 뉴스 제목
  private LocalDateTime publishedAt; // 발행일 (원본 날짜 형식 저장)
  private String summaryText; // 3줄 요약 내용
  private String detailSummary; // 긴 요약
  private String analysisJson; // 감정/편향/신뢰도 통합
  private String keywordsJson;
  private LocalDateTime createdAt; // 생성일
}
