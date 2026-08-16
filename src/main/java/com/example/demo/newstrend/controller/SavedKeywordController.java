package com.example.demo.newstrend.controller;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.dto.request.SavedKeywordRequest;
import com.example.demo.newstrend.entity.SavedKeyword;
import com.example.demo.newstrend.service.SavedKeywordService;


@RestController
@RequestMapping("/keyword")
public class SavedKeywordController {
  @Autowired
  private SavedKeywordService savedKeywordService;

  // 키워드 저장
  @PostMapping("/create")
  public ResponseEntity<SavedKeyword> createKeyword(@RequestBody SavedKeywordRequest request) throws Exception {
    SavedKeyword keyword = savedKeywordService.saveKeyword(request);
    return ResponseEntity.ok(keyword);
  }

  // 키워드 전체 조회
  @GetMapping("/list")
  public ResponseEntity<List<SavedKeyword>> getSavedKeywordList(@RequestParam("memberId") int memberId) {
    List<SavedKeyword> list = savedKeywordService.getAllSavedKeyword(memberId);
    return ResponseEntity.ok(list);
  }

  // 삭제
  @DeleteMapping("/delete")
  public ResponseEntity<Integer> deleteSavedKeyword(@RequestParam("savedKeywordId") int savedKeywordId) throws Exception{
    return ResponseEntity.ok(savedKeywordService.removeSavedKeyword(savedKeywordId));
  }
}
