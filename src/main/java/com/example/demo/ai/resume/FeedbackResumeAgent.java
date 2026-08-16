package com.example.demo.ai.resume;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/*
    이력서 종합 피드백하는 에이전트
*/
@Component
@Slf4j
public class FeedbackResumeAgent {
    private ChatClient chatClient;
    @Autowired
    private ObjectMapper mapper;
    private MemberDao memberDao;
    private ResumeDao resumeDao;

    public FeedbackResumeAgent(ChatClient.Builder chatClientBuilder, MemberDao memberDao, ResumeDao resumeDao) {
        this.chatClient = chatClientBuilder.build();
        this.memberDao = memberDao;
        this.resumeDao = resumeDao;
    }

    public ResumeReportResponse analyze(int memberId, int resumeId) throws Exception {

        // 이력서 조회
        Resume resume = resumeDao.selectResumeById(resumeId);
        if (resume == null) {
            throw new RuntimeException("Resume not found");
        }

        // 멤버 조회(희망 직무)
        Member member = memberDao.findById(memberId);

        // 출력 변환기 생성
        BeanOutputConverter<ResumeReportResponse> converter = new BeanOutputConverter<>(ResumeReportResponse.class);

        String format = converter.getFormat();

        // 프롬프트
        String systemPrompt = """
                당신은 "AI 취업 컨설턴트"입니다.
                지원자의 이력서를 기반으로 채용 담당자의 관점에서 전문적으로 분석하세요.

                출력은 반드시 JSON 형식으로만 작성해야 하며,
                저장된 format 구조 외의 필드는 절대 추가하지 마세요.

                출력 규칙 (절대 위반 금지)
                - format 구조만 출력(추가/누락 금지)
                - JSON key는 선언된 그대로 사용
                - JSON 밖의 다른 문장/텍스트 출력 금지
                - 설명은 한국어로 작성
                - 모든 문장은 공식 보고서 톤의 ‘~합니다’ 형태로 작성(‘~한다’ 금지)

                부적절한 입력 처리 규칙:
                만약 사용자 입력이 다음 중 하나라도 해당하면 “평가 불가”로 처리해야 합니다:
                - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
                - 문장 구조가 없는 단편적 단어
                - 욕설, 비속어, 공격적 표현
                - 이력서로 볼 수 없는 내용
                - 항목 전체가 비어 있거나 공란인 경우

                ※ 항목 역할은 아래 정의를 그대로 따른다. 임의로 재해석하거나 다른 의미를 부여하지 마라.
                분석 항목:
                1) score.careerScore: 경력 기술의 구체성 및 전문성(0~100)
                2) score.matchScore: 희망 직무와의 적합도(0~100)
                3) score.completionScore: 전체 이력서 완성도(0~100)
                4) strengths: 강점
                5) weaknesses: 개선점
                6) portfolioSuggestions: 포트폴리오 프로젝트에서 추가하면 좋은 내용
                7) coverLetterSuggestions: 자기소개서 작성시 강조하면 좋은 내용

                내용 작성 규칙(출력 밀도 강제):
                - score.* 는 반드시 0~100 사이 “정수 숫자만” 출력한다(설명 문장 절대 금지).
                - strengths / weaknesses 는 “불릿 리스트 형태”로 작성한다.
                  - strengths: 최소 3개, 최대 6개
                  - weaknesses: 최소 3개, 최대 6개
                  - 각 불릿은 1~2문장으로 작성하며, 추상적 표현 금지(‘좋습니다/부족합니다’만 쓰지 마라).
                  - 반드시 근거(이력서에서 확인되는 요소) + 의미(채용 관점 영향)까지 포함한다.
                - portfolioSuggestions / coverLetterSuggestions 도 “불릿 리스트 형태”로 작성한다.
                  - 각 항목 최소 1개, 최대 6개
                  - 각 불릿은 1~2문장으로 작성하며, 실행 가능한 구체 지시로 작성한다.
                  - “무엇을/어떻게/어떤 결과물로”가 드러나야 한다.

                지정된 JSON format에 맞춰 분석 결과를 출력하세요.

                format:
                %s
                """.formatted(format);

        String prompt = """
                다음은 사용자의 이력서 데이터입니다.

                --이력서--
                [학력]
                %s
                [경력]
                %s
                [기술]
                %s
                [자격증 및 수상]
                %s
                [활동]
                %s
                [희망 직군]
                %s
                [희망 직무]
                %s
                """.formatted(
                resume.getEducationInfo(),
                resume.getCareerInfo(),
                resume.getSkills(),
                resume.getCertificates(),
                resume.getActivities(),
                member != null ? member.getJobGroup() : "",
                member != null ? member.getJobRole() : "");
        // LLM 호출
        String json = chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .call()
                .content();

        // JSON -> DTO 변환
        ResumeReportResponse result = converter.convert(json);

        // DTO -> JSON 문자열로 직렬화하여 저장
        String feedbackJson = mapper.writeValueAsString(result);
        resumeDao.updateResumeFeedback(resumeId, feedbackJson);

        return result;
    }

}
