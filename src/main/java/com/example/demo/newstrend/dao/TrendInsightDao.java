package com.example.demo.newstrend.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.newstrend.entity.TrendInsight;

@Mapper
public interface TrendInsightDao {
  // LLM 생성 결과 저장
  public int insertTrendInsight(TrendInsight trendInsight);

  // 최신 인사이트 조회
  public TrendInsight selectLatestTrendInsight(int memberId);

  // 단건 삭제
  public int deleteTrendInsight(int trendId);

  // 전체 삭제
  public int deleteTrendInsightByMember(int memberId);
}
