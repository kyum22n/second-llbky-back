package com.example.demo.coverletter.dto.response;

import lombok.Data;

// 자소서 생성 응답
@Data
public class CoverLetterCreateResponse {
    private int coverletterId;  //자소서 ID
    private CoverLetterFinalFeedback finalFeedback; //자소서 최종 피드백
}
