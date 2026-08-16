package com.example.demo.interview.dto.response;

import java.util.List;

import com.example.demo.interview.entity.InterviewSession;

import lombok.Data;

// 리포트 상세 조회용 조합 DTO

@Data
public class InterviewReportResponse {
    private InterviewSession sessionInfo;   // 면접 정보
    private SessionFeedbackResponse finalFeedback;  // 면접 종합 피드백
    private List<InterviewQAResponse>  qaList; // 질문 + 답변 + 답변별 피드백
}
