package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.dto.response.MemoCheckResponse;
import com.example.demo.learning.entity.LearningDay;

// 사용자가 작성한 메모를 다시 정리해주는 에이전트

@Component
public class RewriteMemoAgent {

    // DAO
    @Autowired
    private LearningDayDao learningDayDao;

    // ChatClient
    private ChatClient chatClient;

    public RewriteMemoAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 메모 작성
    public LearningDay execute(LearningDay day, String origin, MemoCheckResponse checkResult) {
        
        // 프롬프트 구성
        // 시스템
        String sysPrompt = """
            당신은 전문 학습 코치입니다.

            다음 규칙을 반드시 따르세요:

            1) MemoCheckResult.isValid 가 false일 경우:
                - 사용자가 입력한 메모를 절대 수정하거나 정리하지 않는다.
                - 아래 형식으로 '안내문'만 출력한다:

                    [학습 기록 거부 안내]
                    - 입력하신 메모는 오늘의 학습 주제와 충분히 관련이 없습니다.
                    - 사유: %s
                    - 올바른 학습 기록을 위해 오늘의 학습 내용을 확인한 뒤 다시 작성해주세요.

                - 절대로 다른 내용이나 정리된 메모를 출력하지 않는다.

            2) MemoCheckResult.isValid 가 true일 경우:
                - 사용자가 작성한 메모(origin)를 깔끔하고 간결하게 정리한다.
                - 불필요한 문장은 제거하고 핵심만 반영하며
                사용자가 이해하기 쉽도록 구성한다.
        """.formatted(checkResult.getReason());

        // 사용자
        String userPrompt = """
            
            [오늘의 학습 주제]            
            %s

            [사용자 작성 메모 원본]
            %s

            [검증 결과]
            - isValid: %s
            - reason: %s
            - summary: %s

        """.formatted(
            day.getTitle(), // 제목
            origin, // 사용자 메모
            checkResult.getIsValid(),   // 검증 결과
            checkResult.getReason(),    // 사유
            checkResult.getSummary()    // 메모 요약
        );

        // AI 응답 받기
        String newMemo = chatClient.prompt()
            .system(sysPrompt)
            .user(userPrompt)
            .call()
            .content();

        if (newMemo == null || newMemo.isBlank()) {
            newMemo = origin;
        }

        // DB에 메모 저장
        day.setLearningDaySummary(newMemo);
        
        // 검증 실패/성공 상태 처리 분기
        Boolean valid = checkResult.getIsValid() != null ? checkResult.getIsValid() : false;

        if (!valid) {
            day.setStatus("진행 중");
        } else {
            day.setStatus("완료");
        }

        // 일일 학습 테이블에서 learningDaySummary랑 status만 업데이트
        learningDayDao.update(day);

        return day;
    }
}
