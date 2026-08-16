package com.example.demo.learning.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.learning.entity.Learning;

@Mapper
public interface LearningDao {
  // 학습 로드맵 생성
  public int insert(Learning learning);

  // LearningId로 학습 상세 조회
  public Learning selectedByLearningId(Integer learningId);

  // 사용자별 학습 전체 리스트 조회
  public List<Learning> selectListByMemberId(Integer memberId);

  // 학습 상태에 따른 리스트 조회
  public List<Learning> selectListByStatus(@Param("memberId") Integer memberId, @Param("status") String status);

  // 사용자의 학습 학습 상태별 개수 조회
  public int countByMemberId(Integer memberId, @Param("status") String status);

  // 학습 상태 변경
  public int update(Learning learning);
}

