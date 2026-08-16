package com.example.demo.newstrend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.service.TrendService;





@RestController
@RequestMapping("/trend")
public class TrendController {
  @Autowired
  private TrendService trendService;
  
  // 오늘 데이터 없으면 자동 분석 -> 있으면 DB 반환
  @GetMapping("/today")
  public ResponseEntity<TrendAnalyzeResponse> getTodayTrend(@RequestParam("memberId") Integer memberId) throws Exception {
    return ResponseEntity.ok(trendService.getAnalyzeToday(memberId));
  }
}
