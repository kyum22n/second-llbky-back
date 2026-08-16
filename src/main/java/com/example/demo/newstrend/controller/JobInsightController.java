package com.example.demo.newstrend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.entity.JobInsight;
import com.example.demo.newstrend.service.JobInsightService;


@RestController
@RequestMapping("/jobInsight")
public class JobInsightController {
  @Autowired
  private JobInsightService jobInsightService;

  // 직무 인사이트 분석
  @PostMapping("/generate")
  public ResponseEntity<JobInsight> generateJobInsight(@RequestParam("memberId") int memberId) throws Exception {
    return ResponseEntity.ok(jobInsightService.createJobInsight(memberId));
  }

  // 최신 직무 인사이트 1개 조회
  @GetMapping("/latest")
  public ResponseEntity<JobInsight> getJobInsight(@RequestParam("memberId") int memberId) {
    return ResponseEntity.ok(jobInsightService.getLatestInsight(memberId));
  }

  // 조회시 생성 기간 7일 지날시 재분석
  @GetMapping("/week")
  public ResponseEntity<JobInsight> analyze(@RequestParam("memberId") int memberId) throws Exception {
    return ResponseEntity.ok(jobInsightService.getOrCreateInsight(memberId));
  }

  // 키워드 저장/삭제 시 성장 제안 수정
  @PostMapping("/refresh")
  public ResponseEntity<JobInsight> updateGrowth(@RequestParam("memberId") int memberId) throws Exception {
    return ResponseEntity.ok(jobInsightService.modifyGrowthAnalysis(memberId));
  }
}
