package com.example.demo.ai.coverletter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.dto.response.CoverLetterFinalFeedback;
import com.example.demo.coverletter.entity.CoverLetter;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;

// 자소서 최종 피드백 생성하고 저장하는 에이전트

@Component
public class FinalFeedbackAgent {
    
    // DAO
    @Autowired
    private CoverLetterDao coverLetterDao;
    @Autowired
    private MemberDao memberDao;

    // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;

    // ChatClient
    private ChatClient chatClient;
    
    // ChatClientBuilder 주입
    public FinalFeedbackAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 종합 피드백 생성
    public CoverLetterFinalFeedback execute(int coverletterId) throws Exception {
        
        // 1. DB에서 자소서 불러오기
        CoverLetter coverLetter = coverLetterDao.selectOneCoverLetter(coverletterId);
        if (coverLetter == null) {
            throw new RuntimeException("CoverLetter not found");
        }

        // 사용자 ID 불러오기
        Member member = memberDao.findById(coverLetter.getMemberId());

        // 2. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<CoverLetterFinalFeedback> converter = new BeanOutputConverter<>(CoverLetterFinalFeedback.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 3. 프롬프트 구성
        // 시스템
        String systemPrompt = """
            당신은 전문 취업 컨설턴트이며, 문장 분석·직무 역량 평가·논리적 흐름·면접관 관점의 서류 검수에 능숙한 전문가입니다. 
            톤은 따뜻하거나 무난한 조언이 아니라, 지원자를 성장시키기 위한 ‘냉정하고 직설적인 전문가 코멘트’ 스타일로 답변해야 합니다.
            지나친 감정 표현, 공감 문구, 부드러운 문장 사용을 절대 포함하지 않습니다.

            평가 규칙:
            - 각 항목 점수는 0~100점 사이 정수로 계산한다.
            - 점수는 명확한 근거 기반으로 설명해야 한다.
            - 강점과 개선점은 실제 서류 수정에 활용 가능한 구체적 문장으로 작성한다.
            - 개선 제안에는 반드시 ‘이후 행동(Action Plan)’이 포함되어야 한다.
            - JSON 형식 이외의 불필요한 문장을 출력하지 않는다.

            부적절한 입력 처리 규칙:
            만약 사용자 입력이 다음 중 하나라도 해당하면 “평가 불가”로 처리해야 합니다:
            - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
            - 문장 구조가 없는 단편적 단어
            - 욕설, 비속어, 공격적 표현
            - 자소서 항목으로 볼 수 없는 내용
            - 항목 전체가 비어 있거나 공란인 경우

            모든 문장은 공식 보고서 톤인 ‘~합니다’ 형태로 작성하며, ‘~한다’와 같은 평서형은 절대 사용하지 않습니다.

            아래 JSON 형식을 정확히 준수해 답변할 것:
            %s
        """.formatted(format);

        // 사용자
        String prompt = """
            지원자의 희망 직군은 "%s", 희망 직무는 "%s"입니다.
            이 정보를 기반으로 자소서 전체를 종합 평가하세요.

            분석 항목:
            1) 문장 분석 (문법, 가독성, 논리 흐름, STAR 기법 적용 여부)
            2) 강점 3가지
            3) 개선 제안 3가지 (반드시 Action Plan 포함)

            --- 자기소개서 내용 ---
            [지원동기]
            %s

            [성장경험]
            %s

            [직무 역량]
            %s

            [입사 후 포부]
            %s
        """.formatted(
                member != null ? member.getJobGroup() : "정보 없음",
                member != null ? member.getJobRole() : "정보 없음",
                coverLetter.getSupportMotive(),
                coverLetter.getGrowthExperience(),
                coverLetter.getJobCapability(),
                coverLetter.getFuturePlan()
        );

        // 4. LLM 호출
        String json = chatClient.prompt()
            .system(systemPrompt)
            .user(prompt)
            .call()
            .content();

        // 5. JSON -> DTO 변환
        CoverLetterFinalFeedback feedback = converter.convert(json);

        // 6. DTO -> JSON으로 다시 직렬화해서 DB 업데이트
        String jsonString = objectMapper.writeValueAsString(feedback);
        coverLetter.setCoverFeedback(jsonString);
        coverLetterDao.updateCoverLetter(coverLetter);

        return feedback;
    }

}
