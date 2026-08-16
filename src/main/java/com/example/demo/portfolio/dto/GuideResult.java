package com.example.demo.portfolio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * AI 포트폴리오 코칭 결과 DTO
 * LLM이 생성한 실시간 피드백 결과를 담는 구조
 * 
 * 이 객체는 두 가지 용도로 사용됨:
 * 1. 실시간 코칭 API 응답
 * 2. guide_feedback(JSONB) 필드에 저장
 * agent에서 builder 패턴 사용해서 @Builder 사용함
 */

@Data
@Builder
public class GuideResult {
     
    private boolean success;//코칭 성공 여부

    private String coachingMessage;//AI 코칭 메시지
     
    private Integer appropriatenessScore;//입력 내용 적절성 점수 (1-10)
    
    private Integer progressPercentage;//현재 진행률 (%)
       
    private List<String> suggestions;//개선 제안사항 구체적이고 실행 가능한 개선 방안 3-5개
  
    private List<String> examples;//예시 문장/내용 2~3개
   
    private String nextStepGuide;//다음 단계 가이드:현재 항목 완성 후 다음에 작성할 내용 안내
    
    private String errorMessage;//오류 메시지
    
}
