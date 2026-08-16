package com.example.demo.resume.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.resume.entity.Resume;

@Mapper
public interface ResumeDao {

  public int insertResume(Resume resume);//이력서 생성

  public Resume selectResumeById(@Param("resumeId") int resumeId);//이력서 상세 조회

  public List<Resume> selectResumesByMemberId(@Param("memberId") int memberId);//회원별 이력서 목록 조회

  public int updateResume(Resume resume);//이력서 수정 (전체)

  public int updateResumeFeedback(
    @Param("resumeId") Integer resumeId,
    @Param("feedback") String feedback);//AI 피드백만 업데이트

  public int deleteResume(int resumeId);//이력서 삭제

}