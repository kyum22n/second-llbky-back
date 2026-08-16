package com.example.demo.coverletter.dto.request;

import java.util.List;

import lombok.Data;

// 실시간 코칭 요청
@Data
public class CoverLetterCoachRequest {
    private int memberId;   //사용자 ID
    private String section; //항목
    private String content; //사용자가 입력한 내용
    private List<String> keywords; // 선택한 키워드
}
