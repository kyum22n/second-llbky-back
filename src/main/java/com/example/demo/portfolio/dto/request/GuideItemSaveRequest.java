package com.example.demo.portfolio.dto.request;

import com.example.demo.portfolio.dto.GuideResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별 항목 저장 요청 DTO
 * 사용자가 특정 항목을 완료했을 때 해당 내용을 가이드에 저장
 */
@Data
public class GuideItemSaveRequest {
    /**
     * 가이드 ID
     */
    private Integer guideId;
    
    /**
     * 단계 번호 (1, 2, 3...)
     */
    private Integer stepNumber;
    
    /**
     * 단계 제목 (예: "프로젝트 개요", "핵심 역량 & 기술")
     */
    private String stepTitle;
    
    /**
     * 항목 제목 (예: "프로젝트 제목", "프로젝트 기간")
     */
    private String itemTitle;
    
    /**
     * 사용자가 입력한 내용 (원본 또는 AI 예시 선택)
     */
    private String itemContent;
    
    /**
     * 항목 상태 ("미작성", "작성 중", "완료")
     */
    private String itemStatus;
    
    /**
     * AI 피드백 (선택사항 - 피드백을 받은 경우에만)
     */
    private GuideResult feedback;
}
