package com.example.demo.learning.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.learning.entity.LearningDay;

@Mapper
public interface LearningDayDao {
  // 학습 로드맵 생성 - 일차별
  public int insert(LearningDay learningDay);

  // dayId로 학습 조회
  public LearningDay selectedByDayId(int dayId);

  // 주차별 전체 리스트 조회
  public List<LearningDay> selectListByWeekId(int weekId);

  // 특정 학습 플랜 전체 일차 목록 조회 (AI 분석 등에서 필요)
  public List<LearningDay> selectListByLearningId(int learningId);

  // 특정 주차에서 특정 일차 조회 ex) 1일차
  public LearningDay selectByWeekIdAndDayNumber(@Param("weekId") int weekId, @Param("dayNumber") int dayNumber);

  // 학습 상태 변경
  public int update(LearningDay learningDay);
}
