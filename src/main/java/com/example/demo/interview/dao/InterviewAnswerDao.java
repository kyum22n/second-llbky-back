package com.example.demo.interview.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.interview.entity.InterviewAnswer;

@Mapper
public interface InterviewAnswerDao {

  // 답변 제출
  public int insertInterviewAnswer(InterviewAnswer interviewAnswer);

  // 답변 다시 제출
  public int updateInterviewAnswer(InterviewAnswer interviewAnswer);

  // 면접 질문 선택 시 연관된 답변 조회
  public InterviewAnswer selectInterviewAnswerByQuestionId(int questionId);

  // 답변 ID로 답변 조회
  public InterviewAnswer selectOneAnswer(int answerId);

  // 답변 STT로 변환된 텍스트 업데이트
  public int updateAnswerText(@Param("answerId") int answerId, @Param("answerText") String answerText);

  // 답변 피드백 업데이트
  public int updateAnswerFeedback(@Param("answerId") int answerId, @Param("answerFeedback") String answerFeedback);

}
