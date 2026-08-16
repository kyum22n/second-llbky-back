package com.example.demo.coverletter.dto.response;

import lombok.Data;

// 문체 버전 응답
@Data
public class WritingStyle {
    private String simpleVersion;   //간결한 버전
    private String caseVersion; //사례 중심 버전
    private String visionVersion;   //비전 제시형 버전
}
