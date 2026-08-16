package com.example.demo.resume.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class ResumeCoachRequest {
  private int memberId;
  private String section;
  private String content;
  private List<String> keywords; // 선택한 키워드

  // 경력일 때 사용
  private String company; // 회사명
  private String position; // 직무
  private String startDate; // 입사일
  private String endDate; // 퇴사일
  private Boolean isCurrent; // 재직 여부

  // 활동일 때 사용
  private String activityName; // 활동명
  private String organization; // 기관/단체명
  private String activityStart; // 활동 시작일
  private String activityEnd; // 활동 종료일
}
