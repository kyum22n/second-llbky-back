package com.example.demo.portfolio.dto.request;

import lombok.Builder;
import lombok.Data;
/*
   @Builder 사용 이유: 가이드컨트롤러에서 /feedback 용 으로 빌드 패턴 사용해서 
   요청 클래스에서 빌드 어노테이션 필요함
*/


@Data
@Builder
public class GuideRequest {
    
    private Integer guideId;//가이드 ID
   
    private Integer memberId;// 회원 ID

    private String title;
    
    private Integer standardId;//평가 기준 ID (어떤 standard로 평가할지)

    private Integer currentStep;//현재 단계 (1-5단계)
    
    private String userInput;//사용자가 입력한 내용
    
    private String inputFieldType;//입력 필드 타입 (제목, 기간, 목적 등)
    
    /**
     * 직무/직군 정보 (AI 코칭 맞춤화용)
     */
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
}
