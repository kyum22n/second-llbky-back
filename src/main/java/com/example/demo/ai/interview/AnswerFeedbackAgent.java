package com.example.demo.ai.interview;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.fasterxml.jackson.databind.ObjectMapper;

// 면접 진행 에이전트 (답변별 피드백 생성)

@Component
public class AnswerFeedbackAgent {

    /*==============
      필드
    =============== */

    // ChatClient
    private ChatClient chatClient;

    // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;
    
    // DAO
    @Autowired
    private InterviewAnswerDao interviewAnswerDao;
    
    
    /*==============
      생성자
    =============== */
    public AnswerFeedbackAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    

    /*==============
      메소드
    =============== */

    // 답변별 피드백 생성
    @Transactional
    public AnswerFeedbackResponse execute(int answerId, String answerText, List<String> visualFeedbackList) throws Exception {
        
        // 1. DB에서 답변 불러오기
        InterviewAnswer answer = interviewAnswerDao.selectOneAnswer(answerId);

        if (answer == null) {
            throw new RuntimeException("Answer not found");
        }

        
        // 2. 이미지 분석 리스트 -> 하나의 문자열로 변환
        String visualFeedbackText;
        if (visualFeedbackList == null || visualFeedbackList.isEmpty()) {
            visualFeedbackText = "영상 분석 없음";
        } else {
            // 프레임 별로 나온 피드백을 하나의 문자열로 합치기
            StringBuilder sb = new StringBuilder(); 
            for (int i = 0; i < visualFeedbackList.size(); i++) {
                sb.append("프레임 ").append(i + 1).append(" 분석: ");
                sb.append(visualFeedbackList.get(i));
                sb.append("\n");
            }

            visualFeedbackText = sb.toString();
        }


        // 3. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<AnswerFeedbackResponse> converter = new BeanOutputConverter<>(AnswerFeedbackResponse.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 4. 프롬프트 구성
        // 시스템
        String systemPrompt = """
            당신은 전문 면접관입니다.
            지원자의 면접 답변(언어적 요소)과 이미지 기반 비언어적 분석 결과를 활용하여
            면접 평가 및 피드백을 생성하는 역할을 수행합니다.

            ========================================================
            [출력 규칙 – JSON ONLY]
            최종 출력은 반드시 아래 JSON 형식을 *그대로* 따라야 합니다.
            JSON 외의 문장은 절대 출력하지 않습니다.

            - languageScore: 0~100 (언어적 요소 기반)
            - nonLanguageScore: 0~100 (비언어적 요소 기반)
            - totalScore: (languageScore + nonLanguageScore) / 2
            - overallSummary: 전체 요약
            - keyCoachingPoint: 핵심 개선 포인트
            - speechAnalysis: 말투·발음·표현 습관 분석
            - toneExpressionAnalysis: 표정·태도·시선 등 비언어적 분석
            - timeStructureAnalysis: 말하는 구조·시간 배분 분석
            - contentAnalysis: 답변 내용의 깊이·전문성 분석

            ========================================================
            [언어 분석 규칙]

            ★ speechAnalysis 규칙
            - 말투, 발음, 속도, 억양, 표현습관 등 "언어적 행동"만 평가
            - 절대 논리 구조, 전문성, 내용 평가는 포함하지 않음

            ★ contentAnalysis 규칙
            - 답변의 내용, 논리적 구조, 전문성, 사례 언급 등만 평가
            - 절대 발음·말투·속도 등 언어 습관 표현 포함 금지

            두 항목은 절대로 중복되면 안 됩니다.

            ========================================================
            [비언어 분석 규칙]

            toneExpressionAnalysis 항목에만 visualFeedbackText 내용을 사용해야 합니다.  
            visualFeedbackText는 다른 항목(speechAnalysis, timeStructureAnalysis, contentAnalysis)에 절대로 포함하면 안 됩니다.

            ========================================================
            [비언어 분석 없음 규칙 — 최우선 적용]

            다음 조건일 때는 “영상 정보 없음” 상태로 처리합니다:
            - visualFeedbackText가 빈 문자열("") 이거나
            - 영상 프레임이 존재하지 않는 경우

            이때는 반드시 아래 규칙을 적용합니다:

            1) nonLanguageScore = 0  
            2) toneExpressionAnalysis = "영상 정보가 없어 비언어적 분석을 제공할 수 없습니다."  
            3) totalScore = languageScore (반드시 동일하게 설정할 것)
            4) visualFeedbackText는 어떤 분석에도 사용하지 않는다  

            ※ 이 규칙은 모든 규칙 중 최우선으로 적용됩니다.

            ========================================================
            [언어적 답변 부족(STT 불량) 규칙]

            answerText가 아래 중 하나면 "언어적 답변 없음"으로 간주합니다:
            - null 또는 빈 문자열
            - 1~3 단어 이하
            - "음", "어", "하" 등 의미 없는 발성
            - 잡음만 존재하는 경우

            이 경우 아래 JSON을 따릅니다:

            {
            "languageScore": 0,
            "nonLanguageScore": (비언어 분석이 있다면 그대로 사용, 없다면 0),
            "totalScore": languageScore,
            "overallSummary": "언어적 답변이 감지되지 않아 내용 기반 분석이 불가합니다.",
            "keyCoachingPoint": "면접 질문에는 최소한 20초 이상의 언어적 답변이 필요합니다.",
            "speechAnalysis": "언어적 발화가 확인되지 않았습니다.",
            "toneExpressionAnalysis": (비언어 분석이 있으면 결과 사용, 없으면 고정 문구 사용),
            "timeStructureAnalysis": "답변 구조 분석이 불가능합니다.",
            "contentAnalysis": "답변 내용이 존재하지 않아 평가할 수 없습니다."
            }

            ========================================================
            [정상 답변 처리 규칙]

            언어적 답변이 충분하고 비언어 분석도 존재하는 경우,
            모든 항목을 위 평가 기준에 따라 상세히 작성합니다.

            ========================================================
            [문체 규칙]

            - 모든 문장은 ‘~합니다’로 끝나는 공식 보고서 톤으로 작성합니다.
            - ‘~한다’, 구어체, 감정적 표현은 절대 사용하지 않습니다.

            ========================================================
            아래 JSON 형식을 정확히 준수하여 출력하십시오:
            %s

        """.formatted(format);

        // 사용자
        String prompt = """
            다음은 지원자의 면접 답변 내용입니다. 이를 분석하고 피드백을 제공하세요.
            
            [지원자 답변 내용]
            %s

            [이미지 기반 비언어적 분석 결과]
            %s
        """.formatted(answerText, visualFeedbackText);

        // 5. LLM 호출
        String json = chatClient.prompt()
            .system(systemPrompt)
            .user(prompt)
            .call()
            .content();

        // 6. JSON -> DTO 변환
        AnswerFeedbackResponse response = converter.convert(json);

        // 7. DTO -> JSON으로 다시 직렬화해서 DB 업데이트
        String jsonString = objectMapper.writeValueAsString(response);
        interviewAnswerDao.updateAnswerFeedback(answerId, jsonString);

        return response;
        
    }


}
