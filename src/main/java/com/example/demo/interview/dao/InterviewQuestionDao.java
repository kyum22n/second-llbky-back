package com.example.demo.interview.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.entity.InterviewQuestion;

@Mapper
public interface InterviewQuestionDao {
  
  // AI 예상 면접 질문 저장
  public int insertInterviewQuestion(InterviewQuestion interviewQuestion);

  // 사용자 예상 면접 질문 추가
  public int insertCustomQuestion(InterviewQuestion interviewQuestion);

  // 면접 질문 목록 조회
  public List<InterviewQuestion> selectAllInterviewQuestions(int memberId);

  // 면접 리포트 상세보기 시 질문 목록 조회
  public List<InterviewQuestion> selectInterviewQuestionsBySessionId(int sessionId);

}
