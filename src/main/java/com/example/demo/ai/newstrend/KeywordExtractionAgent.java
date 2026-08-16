package com.example.demo.ai.newstrend;


import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 키워드 추출 Agent
 * - 뉴스 요약문에서 구직자에게 유용한 키워드 자동 추출
 * - 키워드 분류 (직무/기술/산업/기타)
 * - 중요도 점수 산출
 */
@Component
@Slf4j
public class KeywordExtractionAgent {
    
    private  ChatClient chatClient;
    private  ObjectMapper objectMapper;
    
    public KeywordExtractionAgent(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * 뉴스 요약문에서 키워드 추출
     * @param summaryText 뉴스 요약문
     * @return 추출된 키워드 리스트 (최대 5개, 타입 및 중요도 포함)
     * @throws Exception JSON 파싱 실패 또는 AI 호출 실패 시
     */
    public List<NewsKeywordResponse> extractKeywords(String summaryText) throws Exception {
        log.info("키워드 추출 시작 - 요약문 길이: {} 글자", summaryText.length());
        
        String systemPrompt = """
            당신은 취업 시장 키워드 분석 전문가입니다.
            뉴스 요약에서 구직자에게 유용한 키워드를 추출합니다.

             ======================================== 
            키워드 추출 기준
            ======================================== 
            - 고유명사: 기업명, 인명, 제품명, 기술명
            - 핵심개념: 주요 이슈, 트렌드
            - 수치정보: 중요한 수치가 포함된 용어
            - 중요도 순서: 뉴스 핵심 → 부가 정보
            - 직무 분야(개발자, 디자이너, 기획자 등)
            - 고용 형태(인턴, 신입, 경력 등)
            - 산업 분야(IT, AI, 기술 등)
            도 함께 추출하세요
          
            
            추출 규칙:
            1. 5개 이내의 핵심 키워드만 추출
            
            
            JSON 배열로만 응답:
            [
              {"keyword": "AI"},
              {"keyword": "백엔드"}
            ]
        """;
        
        String userPrompt = """
            다음 뉴스 요약에서 키워드를 추출하세요:
            
            %s
        """.formatted(summaryText);
        
        log.debug("AI 호출 시작");
        
        String jsonResponse = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();
        
        log.debug("AI 응답 수신 - 길이: {} bytes", jsonResponse.length());
        
        // JSON 배열 파싱
        List<NewsKeywordResponse> keywords = objectMapper.readValue(
            jsonResponse, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, NewsKeywordResponse.class)
        );
        
        log.info("키워드 추출 완료 - 추출된 키워드 수: {}", keywords.size());
        
        if (!keywords.isEmpty()) {
            log.debug("추출된 키워드:");
            for (NewsKeywordResponse keyword : keywords) {
                log.debug("  - {}", keyword.getKeyword());
            }
        }
        
        return keywords;
    }
}
