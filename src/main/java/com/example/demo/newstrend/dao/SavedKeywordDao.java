package com.example.demo.newstrend.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.newstrend.entity.SavedKeyword;

@Mapper
public interface SavedKeywordDao {
  // 키워드 저장
  public int insertSavedKeyword(
      @Param("memberId") int memberId,
      @Param("keyword") String keyword,
      @Param("sourceLabel") String sourceLabel);

  // 멤버 키워드 전체 조회
  public List<SavedKeyword> selectSavedKeywordListByMemberId(int memberId);

  // 개별 키워드 조회
  public SavedKeyword selectSavedKeywordById(int savedKeywordId);

  // 키워드 중복 체크
  public SavedKeyword selectSavedKeyword(
      @Param("memberId") int memberId,
      @Param("keyword") String keyword);

  // 삭제
  public int deleteSavedKeyword(int savedKeywordId);

}
