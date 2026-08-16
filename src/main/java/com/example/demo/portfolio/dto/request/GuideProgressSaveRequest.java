package com.example.demo.portfolio.dto.request;

import java.util.List;

import com.example.demo.portfolio.dto.GuideStepData;

import lombok.Data;

/**
 * 전체 가이드 진행상황 저장 요청 DTO
 * 사용자가 "진행상황 저장" 버튼을 클릭했을 때 모든 내용을 저장
 */
@Data
public class GuideProgressSaveRequest {
    
    private Integer guideId;//가이드 ID

    private Integer memberId;//회원 ID

    private Integer completionPercentage;//전체 진행률 (0-100%)

    private Integer currentStep;//현재 진행 중인 단계 번호

    private List<GuideStepData> guideContent;//전체 가이드 내용 (모든 단계와 항목)

    private List<GuideStepData> guideSteps;

   
}
