package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

// 뉴스에서 최종 추출된 트렌드 분석용 키워드
@Data
public class TrendKeywordResponse {
    private List<TrendKeywordItem> keywords;
}
