package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스와 직군 간의 관련성을 점수화하는 Agent
 * - 뉴스가 특정 직군과 얼마나 연관되는지 0~100점으로 평가
 * - 점수(숫자)만 반환
 */
@Component  
@Slf4j      
public class JobRelevanceAgent {

    private final ChatClient chatClient;  // Spring AI ChatClient (불변 필드, 생성자 주입)

    // 생성자: ChatClient.Builder를 주입받아 ChatClient 인스턴스 생성
    public JobRelevanceAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();  // 빌더 패턴으로 ChatClient 생성
    }

    /**
     * 수집된 뉴스(분석 전)의 관련성을 평가
     * 
     * @param news 수집된 뉴스 정보 (제목, 내용만 있는 상태)
     * @param jobCategory 평가할 직군 (예: "개발", "기획", "마케팅")
     * @return 관련성 점수 (0~100, 파싱 실패 시 0)
     */
    public int calculateRelevanceScore(NewsAnalysisRequest news, String jobCategory) {
        // INFO 레벨 로그: 수집된 뉴스와 직군 정보 기록
        log.info("수집된 뉴스 관련도 점수 계산 - jobCategory: {}, title: {}", jobCategory, news.getTitle());

        // AI 시스템 프롬프트 (AI의 역할과 평가 기준 정의)
        // Text Block (""")로 여러 줄 문자열 작성
        String systemPrompt = """
            당신은 뉴스 분석과 직무 적합성 평가를 전문적으로 수행하는 분석가입니다.
            입력된 뉴스가 특정 직군과 얼마나 관련되는지 0~100점으로 평가합니다.
            
            평가 기준:
            - 직접적 관련성 (채용, 주요 업무 관련): 80-100점
            - 간접적 관련성 (업계 동향, 전략/기술 흐름): 50-79점
            - 일반적 관심사 (경제, IT 트렌드 등): 20-49점
            - 관련성 낮음: 0-19점
            
            출력 규칙:
            - 정수 숫자만 출력
            - 다른 문장 포함 금지
            """;

        // AI 사용자 프롬프트 (실제 평가 요청)
        // formatted()로 동적 값 삽입 (Java 15+ String formatting)
        String userPrompt = """
            다음 뉴스가 '%s' 직군과 얼마나 관련있는지 평가하세요.
            한국어로 기사를 찾아주세요
            제목: %s
            내용: %s
            출처: %s
            
            점수만 숫자로 출력하세요.
            """.formatted(
                    jobCategory,              // %s 첫 번째: 직군
                    news.getTitle(),          // %s 두 번째: 뉴스 제목
                    // %s 세 번째: 뉴스 본문 (500자로 제한, null일 경우 "내용 없음")
                    // substring()으로 본문 길이 제한 (토큰 절약 + 응답 속도 향상)
                    // Math.min()으로 500과 실제 길이 중 작은 값 선택 (IndexOutOfBounds 방지)
                    news.getContent() != null ? news.getContent().substring(0, Math.min(500, news.getContent().length())) : "내용 없음",
                    news.getSourceName()      // %s 네 번째: 뉴스 출처 (예: "네이버", "다음")
            );

        // Spring AI ChatClient를 사용한 AI 호출
        String aiResult = chatClient.prompt()     // 프롬프트 생성 시작
                .system(systemPrompt)              // 시스템 메시지 설정 (AI 역할)
                .user(userPrompt)                  // 사용자 메시지 설정 (실제 요청)
                .call()                            // AI 모델 호출 (동기 방식)
                .content()                         // 응답 내용 추출 (String)
                .trim();                           // 앞뒤 공백 제거

        try {
            // 응답에서 숫자만 추출하여 정수로 변환
            // replaceAll("[^0-9]", ""): 숫자가 아닌 모든 문자 제거 (정규표현식)
            // 예: "75점입니다" → "75", "Score: 80" → "80"
            int score = Integer.parseInt(aiResult.replaceAll("[^0-9]", ""));
            
            // INFO 레벨 로그: 계산 완료된 점수 기록
            log.info("수집된 뉴스 관련도 점수: {}", score);
            
            return score;  // 점수 반환
            
        } catch (Exception e) {  // NumberFormatException 등 파싱 오류 처리
            // WARN 레벨 로그: 예외 발생 시 원본 응답과 함께 경고
            log.warn("관련도 점수 파싱 실패. 기본값 0 반환. 응답: {}", aiResult);
            
            return 0;  // 파싱 실패 시 안전한 기본값(0) 반환
        }
    }
}
