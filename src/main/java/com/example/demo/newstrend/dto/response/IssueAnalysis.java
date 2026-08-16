package com.example.demo.newstrend.dto.response;

import lombok.Data;

// 실제 사례에 대한 여론을 근거로 뉴스 분석

@Data
public class IssueAnalysis {
    private String issue;        // 이슈 제목 (예: "쿠팡 개인정보 유출")
    private String sentiment;    // "positive" | "neutral" | "negative"
    private int articleCount;    // 몇 개의 뉴스가 이 이슈 관련인지
    private String impact;       // 예: "전체 부정 신호의 63% 차지"
    private String reason;       // 왜 부정인지? 왜 긍정인지? (구체적 설명)
}
