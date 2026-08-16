package com.example.demo.learning.dto.request;

import java.util.List;

import lombok.Data;

@Data
// 학습 플랜 생성 요청
public class RoadmapRequest {
  private Integer memberId;
  private String jobRole;
  private List<String> purposes;
  private List<String> skills;
  private int studyHours;
}
