package com.example.demo.newstrend.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class TrendGraph {
  private List<String> keywords; // x축 (키워드)
  private List<Integer> counts; // y축 (점수: 분석 기간동안의 검색량 평균값을 기준)
  private Map<String, Object> rawTrendData; // 원본 데이터
}
