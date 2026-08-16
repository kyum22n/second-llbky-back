package com.example.demo.interview.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.entity.InterviewSession;

@Mapper
public interface InterviewSessionDao {

  // 면접 세션 생성
  public int insertInterviewSession(InterviewSession interviewSession);

  // 면접 세션 종료 시 종합 피드백 생성
  public int updateInterviewFeedback(InterviewSession interviewSession);

  // 종료된 면접 세션(면접 리포트) 목록 조회
  public List<InterviewSession> selectAllInterviewSessions(int memberId);

  // 면접 리포트 상세보기 시 면접 정보 조회
  public InterviewSession selectOneInterviewSession(int sessionId);

}
