package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 편향 중립화 Agent
 * - 편향되거나 감정적인 표현을 중립적으로 재작성
 * - 객관적이고 사실 중심의 문장으로 변환
 * - 과장된 표현 완화
 */
@Component
@Slf4j
public class BiasNeutralizationAgent {
    
    private  ChatClient chatClient;
    
    public BiasNeutralizationAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * 편향된 텍스트를 중립적으로 재작성
     * @param biasedText 편향된 원문
     * @return 중립화된 텍스트
     */
    public String neutralizeText(String biasedText) {
        log.info("편향 중립화 시작 - 원문 길이: {} 글자", biasedText.length());
        
        String systemPrompt = """
            당신은 객관적 글쓰기 전문가입니다.
            편향되거나 감정적인 표현을 중립적으로 재작성합니다.
            
            ======================================== 
            편향 감지 기준
            ======================================== 
            다음 중 하나라도 해당되면 biasDetected=true:
            - 감정적 표현 과다 사용 (예: "충격적", "끔찍한", "놀라운" 등)
            - 특정 입장을 일방적으로 옹호
            - 과장된 표현 사용 (예: "사상 최대", "전례 없는" 등)
            - 출처가 불명확한 주장
            



            재작성 규칙:
            1. 사실과 수치는 그대로 유지
            2. 감정적 형용사 제거
            3. 추측성 표현을 사실 중심으로 변경
            4. 과장된 표현 완화
            5. 원문의 핵심 정보는 유지
            
            중립적으로 재작성한 문장만 출력하세요.
        """;
        
        String userPrompt = """
            다음 문장을 중립적으로 재작성하세요:
            
            %s
        """.formatted(biasedText);
        
        log.debug("AI 호출 시작");
        
        String neutralizedText = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            
            .call()
            .content();
        
        log.info("편향 중립화 완료 - 결과 길이: {} 글자", neutralizedText.length());
        log.debug("원문: {}", biasedText);
        log.debug("중립화: {}", neutralizedText);
        
        return neutralizedText;
    }
}
