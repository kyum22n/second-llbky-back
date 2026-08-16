package com.example.demo.coverletter.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.coverletter.entity.CoverLetter;

@Mapper
public interface CoverLetterDao {
  // 자소서 저장 + 종합 피드백 생성
  public int insertCoverLetter(CoverLetter coverLetter);

  // 자소서 목록 조회
  public List<CoverLetter> selectAllCoverLetters(int memberId);

  // 자소서 상세보기
  public CoverLetter selectOneCoverLetter(int coverletterId);

  // 자소서 수정하기
  public int updateCoverLetter(CoverLetter coverLetter);

  // 자소서 삭제하기
  public int deleteCoverLetter(@Param("coverletterId") int coverletterId, @Param("memberId") int memberId);
}
