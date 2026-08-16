package com.example.demo.learning.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class RoadmapRefineRequest {
  private String originalRoadmapJson;
  private String userFeedback;

  // 생성할 때 사용했던 정보
  private String jobRole;
  private List<String> purposes;
  private List<String> skills;
  private int studyHours;
}
