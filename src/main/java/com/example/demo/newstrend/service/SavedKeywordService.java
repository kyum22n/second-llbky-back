package com.example.demo.newstrend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.newstrend.dao.SavedKeywordDao;
import com.example.demo.newstrend.dto.request.SavedKeywordRequest;
import com.example.demo.newstrend.entity.SavedKeyword;

@Service
public class SavedKeywordService {
  @Autowired
  private SavedKeywordDao savedKeywordDao;
  @Autowired
  private JobInsightService jobInsightService;

  // 키워드 저장
  @Transactional
  public SavedKeyword saveKeyword(SavedKeywordRequest request) throws Exception {
    // 중복 체크
    SavedKeyword exists = savedKeywordDao.selectSavedKeyword(request.getMemberId(), request.getKeyword());
    if (exists != null) {
      return exists;
    }

    // 저장
    savedKeywordDao.insertSavedKeyword(request.getMemberId(), request.getKeyword(), request.getSourceLabel());

    // 키워드 추가시 직무 인사이트 재생성
    jobInsightService.modifyGrowthAnalysis(request.getMemberId());
    
    return savedKeywordDao.selectSavedKeyword(request.getMemberId(), request.getKeyword());
  }
  
  // 저장한 키워드 전체 조회
  public List<SavedKeyword> getAllSavedKeyword(int memberId){
    return savedKeywordDao.selectSavedKeywordListByMemberId(memberId);
  }
  
  // 삭제
  @Transactional
  public int removeSavedKeyword(int savedKeywordId) throws Exception{
    // 키워드 정보 가져오기(memberId 조회하기 위해)
    SavedKeyword keyword = savedKeywordDao.selectSavedKeywordById(savedKeywordId);
    if(keyword == null){
      return 0;
    }
    // 키워드 삭제
    int result =  savedKeywordDao.deleteSavedKeyword(savedKeywordId);
    
    // 삭제 -> 직무 인사이트 재생성
    jobInsightService.modifyGrowthAnalysis(keyword.getMemberId());

    return result;
  }
}
