package com.example.demo.newstrend.dto.response;

import lombok.Data;

// 트렌드 키워드 항목
@Data
public class TrendKeywordItem {
    private String keyword;        // "AI", "백엔드 채용"
    private int frequency;         // 뉴스 등장 횟수
    private String reason;          // 왜 트렌드로 적합한지 (짧게)
}
