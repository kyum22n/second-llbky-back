package com.example.demo.learning.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.learning.entity.LearningWeek;

@Mapper
public interface LearningWeekDao {
  // 학습 로드맵 생성 - 주차별
  public int insert(LearningWeek learningWeek);

  // weekId로 학습 조회
  public LearningWeek selectedByWeekId(int weekId);

  // 학습별 주차 리스트 조회
  public List<LearningWeek> selectListByLearningId(int learningId);
  
  // 학습 플랜 + 주차 번호로 조회 ex) 2주차
  public LearningWeek selectByLearningIdAndWeekNumber(@Param("learningId") int learningId, @Param("weekNumber") int weekNumber);

  // 학습 상태 변경
  public int update(LearningWeek learningWeek);
}
