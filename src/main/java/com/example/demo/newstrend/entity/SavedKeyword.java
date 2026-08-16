package com.example.demo.newstrend.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SavedKeyword {
  private Integer savedKeywordId; // 저장한 키워드 ID
  private Integer memberId; // 멤버 ID
  private String keyword; // 키워드
  private String sourceLabel; // 직접추가, 추천 직무 태그 표시용
  private LocalDateTime createdAt; // 생성일
}
