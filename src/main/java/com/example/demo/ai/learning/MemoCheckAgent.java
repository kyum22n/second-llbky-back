package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.response.MemoCheckResponse;
import com.example.demo.learning.entity.LearningDay;

// 일일 학습 노트 검증용 에이전트

@Component
public class MemoCheckAgent {

    // ChatClient
    private ChatClient chatClient;

    public MemoCheckAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 검증
    public MemoCheckResponse execute(LearningDay day, String origin) {

        // 구조화된 출력 변환기 생성
        BeanOutputConverter<MemoCheckResponse> converter = new BeanOutputConverter<>(MemoCheckResponse.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 프롬프트 구성
        // 시스템
        String sysPrompt = """
            당신은 전문 학습 코치이며, 사용자가 작성한 메모가
            오늘의 학습 주제와 충분히 관련 있는지를 검증합니다.

            다만, 사용자의 학습 과정(정리, 발표 준비, 느낀 점, 적용 방향 등)은
            기술 설명이 부족하더라도 학습 주제를 기반으로 작성된 경우
            관련성이 있다고 판단해야 합니다.

            검증 기준(완화 버전):

            1) 다음 중 하나라도 충족하면 isValid = true 로 판단할 수 있습니다:
                - 오늘의 학습 주제와 개념적으로 연결되어 있음
                - 학습 내용을 바탕으로 요약·정리·회고·발표 준비를 작성함
                - 학습 내용을 어떻게 활용하거나 이해했는지 설명함
                - 주제와 간접적 연결이 있어도 “학습 기반 정리”라면 허용

            2) 다음 경우에만 isValid = false 로 판단합니다:
                - 오늘의 학습 주제와 전혀 관련 없는 내용
                - 개인 일상, 감정 기록 등 학습 흐름과 무관한 경우
                - 학습 내용을 잘못 이해하여 명확한 기술적 오류가 포함된 경우
                - 학습 주제를 언급했지만 문맥상 연결이 거의 없는 경우

            3) 사용자가 작성한 메모의 표현 방식은 자유롭게 허용합니다.
            학습 개념을 직접 나열하지 않아도 발표 준비·요약·정리 방식이면 인정합니다.

            4) 결과는 반드시 아래 JSON 형식을 지켜 출력해야 합니다:
            %s
        """.formatted(format);

        // 사용자
        String userPrompt = """   

            [오늘의 학습 주제]
            %s

            [오늘의 학습 내용]
            %s

            [사용자가 입력한 메모]
            %s

            오늘의 학습 주제와 내용을 사용자가 입력한 메모와 비교하여 관련성이 충분한지 검증하고,
            사용자가 입력한 메모에서 부족한 점이나 이상한 내용이 있으면 알려주세요. 

        """.formatted(day.getTitle(), day.getContent(), origin);

        // AI 응답 받기
        String json = chatClient.prompt()
            .system(sysPrompt)
            .user(userPrompt)
            .call()
            .content();

        // JSON -> DTO 변환
        MemoCheckResponse checkResult = converter.convert(json);

        return checkResult;
    }
}
