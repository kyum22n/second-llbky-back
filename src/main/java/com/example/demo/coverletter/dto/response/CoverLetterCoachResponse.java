package com.example.demo.coverletter.dto.response;

import lombok.Data;

// 실시간 코칭 응답
@Data
public class CoverLetterCoachResponse {
    private String summary; //한 줄 요약 코멘트
    private String strengths;   //잘한 점 요약
    private String improvements;    //개선점 요약

    private String improvedText;    //AI 수정본
}
