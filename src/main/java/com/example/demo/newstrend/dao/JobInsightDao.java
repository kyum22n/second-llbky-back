package com.example.demo.newstrend.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.newstrend.entity.JobInsight;

@Mapper
public interface JobInsightDao {
  // LLM 생성 직무 인사이트 저장
  public int insertJobInsight(JobInsight jobInsight);

  // 특정 멤버의 최신 직무 인사이트 1개 조회
  public JobInsight selectLatestJobInsight(int memberId);

  // 키워드 저장/삭제 시 성장 제안 수정
  public int updateAnalysisJson(JobInsight insight);

  // 단건 삭제
  public int deleteJobInsight(int insightId);

  // 전체 삭제
  public int deleteJobInsightByMember(int memberId);

}
