package com.example.demo.interview.dto.response;

import lombok.Data;

// 리포트 상세 조회용 질문 + 답변 + 답변별 피드백 응답 DTO

@Data
public class InterviewQAResponse {
    private int questionId; // 질문 ID
    private int answerId;   // 답변 ID
    private String questionText;    // 질문
    private String answerText;  // 답변
    private AnswerFeedbackResponse answerFeedback;  // 답변 피드백

    private String audioFileName;   // 오디오 파일 이름
    private String audioFileType;   // 오디오 파일 타입
    private byte[]  audioFileData;  // 오디오 파일 데이터

    private String videoFileName;   // 비디오 파일 이름
    private String videoFileType;   // 비디오 파일 타입
    private byte[] videoFileData;   // 비디오 파일 데이터
}
