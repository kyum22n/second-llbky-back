package com.example.demo.ai.coverletter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dto.request.CoverLetterCoachRequest;
import com.example.demo.coverletter.dto.response.CoverLetterCoachResponse;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;

import lombok.extern.slf4j.Slf4j;

// 자소서 작성 시 실시간 코칭해주는 에이전트

@Component
@Slf4j
public class RealtimeCoachingAgent {

    // DAO
    @Autowired
    private MemberDao memberDao;

    // ChatClient
    private ChatClient chatClient;
    
    // ChatClientBuilder 주입
    public RealtimeCoachingAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 실시간 코칭
    public CoverLetterCoachResponse execute(CoverLetterCoachRequest request) {

        // 사용자 ID 얻기
        Member member = memberDao.findById(request.getMemberId());

        // 1. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<CoverLetterCoachResponse> converter = new BeanOutputConverter<>(CoverLetterCoachResponse.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 2. 요청된 section만 추출
        String section = null;
        // 선택된 section을 section에 대입
        switch (request.getSection()) {
            case "supportMotive":
                section = "지원동기";
                break;
            case "growthExperience":
                section = "성장경험";
                break;
            case "jobCapability":
                section = "직무역량";
                break;
            case "futurePlan":
                section = "입사 후 포부";
                break;
            default:
                throw new IllegalArgumentException("Invalid section");
        }

        // 3. 프롬프트 구성
        // 시스템
        String systemPrompt = """
            당신은 취업 준비생의 자기소개서를 첨삭해주는 전문 취업 컨설턴트입니다.

            평가 규칙:
            - JSON 형식 이외의 불필요한 문장을 절대 출력하지 않습니다.
            - 모든 문장은 공식 보고서 톤인 ‘~합니다’ 형태로 작성합니다.
            - ‘~한다’ 형태의 평서형은 금지합니다.

            부적절한 입력 처리 규칙:
            만약 사용자 입력이 다음 중 하나라도 해당하면 “평가 불가”로 처리해야 합니다:
            - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
            - 문장 구조가 없는 단편적 단어
            - 욕설, 비속어, 공격적 표현
            - 자소서 항목으로 볼 수 없는 내용
            - 항목 전체가 비어 있거나 공란인 경우

            아래 JSON 스키마를 정확히 준수하여 답변해야 합니다:
            %s
        """.formatted(format);

        // 사용자
        String prompt = """
            사용자가 방금 작성한 "%s" 항목을 분석하고 피드백을 제공하세요.
            
            [사용자 작성 내용]
            %s
            [선택된 키워드]
            %s

            [지원자 정보]
            - 희망 직군: %s
            - 희망 직무: %s

            이 정보를 기반으로 해당 직무에서 중요하게 평가되는 요소 중심으로 코칭하세요.
        """.formatted(
            section,
            request.getContent(),
            request.getKeywords(),
            member != null ? member.getJobGroup() : "정보 없음",    // 직군 반영
            member != null ? member.getJobRole() : "정보 없음"  // 직무 반영
        );

        log.info("받은 키워드: {}", request.getKeywords());

        // 4. LLM 호출
        String json = chatClient.prompt()
            .system(systemPrompt)
            .user(prompt)
            .call()
            .content();

        // 5. JSON -> DTO 변환
        CoverLetterCoachResponse response = converter.convert(json);

        return response;
    }
}
