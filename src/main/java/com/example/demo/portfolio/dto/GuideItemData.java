package com.example.demo.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 가이드 항목 데이터
 */
@Data
public class GuideItemData {
   
    private String title;//항목 제목

    @JsonAlias({"content", "userInput"})  // 둘 다 받을 수 있음
    private String content;
    
    @JsonProperty(value = "userInput")
    private String userInput;

    private String status;//항목 상태 ("미작성", "작성 중", "완료")

    private GuideResult feedback;//AI 피드백 (선택사항)
}
