package com.example.demo.learning.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learning.dto.request.RoadmapRefineRequest;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;
import com.example.demo.learning.dto.response.LearningDetailResponse;
import com.example.demo.learning.dto.response.LearningResponse;
import com.example.demo.learning.dto.response.RecommendSkillResponse;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.entity.LearningWeek;
import com.example.demo.learning.service.LearningDayService;
import com.example.demo.learning.service.LearningService;
import com.example.demo.learning.service.LearningWeekService;


@RestController
@RequestMapping("/learning")
public class LearningController {

  @Autowired
  private LearningService learningService;
  @Autowired
  private LearningDayService learningDayService;
  @Autowired
  private LearningWeekService learningWeekService; 

  // AI 학습 로드맵 생성
  @PostMapping("/roadmap-create")
  public ResponseEntity<AiCreateRoadmapResponse> createRoadmap(
    @RequestParam("memberId") Integer memberId,
    @RequestParam("purposes") List<String> purposes,
    @RequestParam("skills") List<String> skills,
    @RequestParam("studyHours") int studyHours) {

      AiCreateRoadmapResponse result = learningService.createLearning(memberId, purposes, skills, studyHours);
      return ResponseEntity.ok(result);
    }

  // 학습 로드맵 수정
  @PostMapping("/roadmap-refine")
  public ResponseEntity<AiCreateRoadmapResponse> refineRoadmap(@RequestBody RoadmapRefineRequest request) {
    AiCreateRoadmapResponse result = learningService.refineRoadmap(request);
    return ResponseEntity.ok(result);
  }
    

  
  // AI 학습 로드맵 DB 저장
  @PostMapping("/roadmap-save")
  public ResponseEntity<LearningDetailResponse> saveRoadmap(@RequestBody AiCreateRoadmapResponse roadmap) {
    LearningDetailResponse result = learningService.saveRoadmap(roadmap);
    return ResponseEntity.ok(result);
  }
  
  

  // 사용자가 올린 문서 / 직군,직무로 AI가 추천해주는 기술
  @PostMapping("/recommend-skills")
  public ResponseEntity<RecommendSkillResponse> recommendSkill(@RequestParam("memberId") Integer memberId) {
    RecommendSkillResponse result = learningService.recommendSkillsFromFeedback(memberId);
    return ResponseEntity.ok(result); 
  }


  // 학습 리스트 조회
  @GetMapping("/list")
  public ResponseEntity<List<LearningResponse>> getLearningList(@RequestParam("memberId") Integer memberId, @RequestParam("status") String status) {
    List<LearningResponse> result = learningService.getLearningListByStatus(memberId, status);
    return ResponseEntity.ok(result);
  }
  

  // 학습 상세 조회
  @GetMapping("/detail")
  public ResponseEntity<LearningDetailResponse> getLearningDetail(@RequestParam("learningId") int learningId) {
    LearningDetailResponse result = learningService.getLearningById(learningId);
    return ResponseEntity.ok(result);
  }
  
  
  // 학습 ID 기준 주차 전체 조회
  @GetMapping("/weeks-by-roadmap")
  public ResponseEntity<List<LearningWeek>> getWeekListByLearningId(@RequestParam("learningId") int learningId) {
    List<LearningWeek> result = learningWeekService.getWeekListByLearningId(learningId);
    return ResponseEntity.ok(result);
  }
  
  // 주차 상세 조회
  @GetMapping("/week-detail")
  public ResponseEntity<LearningWeek> getLearningWeekDetail(@RequestParam("weekId") int weekId) {
    LearningWeek result = learningWeekService.getWeekById(weekId);
    return ResponseEntity.ok(result);
  }

  // 일일 학습 조회 (주차별)
  @GetMapping("/day-by-week")
  public ResponseEntity<List<LearningDay>> getLearningDayByWeek(@RequestParam("weekId") int weekId) {
    return ResponseEntity.ok(learningDayService.getDayListByWeekId(weekId));
  }

  // 일일 학습 조회 (일차별)
  @GetMapping("/day-by-day")
  public ResponseEntity<LearningDay> getLearningDayByDayId(@RequestParam("dayId") int dayId) {
    return ResponseEntity.ok(learningDayService.getDayById(dayId));
  }

  // AI 일일 학습 정리 제출
  @PostMapping("/submit-day-summary")
  public ResponseEntity<LearningDay> submitLearningDaySummary(
    @RequestParam("dayId") int dayId,
    @RequestParam("learningDaySummary") String learningDaySummary
  ) {
      
    return ResponseEntity.ok(learningDayService.submitMemo(dayId, learningDaySummary));
  }

  @GetMapping("/count")
  public ResponseEntity<Map<String, Integer>> getLearningCount(@RequestParam("memberId") Integer memberId) {
    return ResponseEntity.ok(learningService.getLearningCount(memberId));
  } 
  


}
