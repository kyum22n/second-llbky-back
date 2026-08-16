package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class JobInsightListResponse {
  private List<JobInsightResponse> insights; // 추천 직무 목록
}
