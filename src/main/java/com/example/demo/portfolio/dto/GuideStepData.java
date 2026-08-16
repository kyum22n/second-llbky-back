package com.example.demo.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 가이드 단계 데이터
 */
@Data
@Builder
public class GuideStepData {
    
    @JsonAlias({"stepNumber", "label"})
    private Integer stepNumber;



    private String stepTitle;//단계 제목

    private Integer stepProgress;//단계별 진행률 (0-100%)
    
    private List<GuideItemData> items;// 단계 내 항목들

   
}
