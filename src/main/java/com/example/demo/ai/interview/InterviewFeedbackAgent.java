package com.example.demo.ai.interview;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.QuestionAnswerBundle;
import com.example.demo.interview.dto.response.SessionFeedbackResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.example.demo.interview.entity.InterviewQuestion;
import com.example.demo.interview.entity.InterviewSession;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;

// 면접 최종 피드백을 생성하고 저장하는 에이전트

@Component
public class InterviewFeedbackAgent {

    // DAO
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private InterviewSessionDao interviewSessionDao;
    @Autowired
    private InterviewQuestionDao interviewQuestionDao;
    @Autowired
    private InterviewAnswerDao interviewAnswerDao;

    // AI 응답 DTO -> JSON 문자열로 직렬화하기 위한 ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;

    // ChatClient
    private ChatClient chatClient;

    public InterviewFeedbackAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 종합 피드백 생성
    public SessionFeedbackResponse execute(int sessionId) throws Exception {

        // 1. DB에서 면접 정보 불러오기
        InterviewSession session = interviewSessionDao.selectOneInterviewSession(sessionId);
        if (session == null) {
            throw new RuntimeException("InterviewSession not found");
        }

        // 2. DB에서 면접 질문 불러오기
        List<InterviewQuestion> questions = interviewQuestionDao.selectInterviewQuestionsBySessionId(sessionId);

        // 3. DB에서 면접 답변 텍스트 & 피드백 불러오기
        List<QuestionAnswerBundle> qaList = new ArrayList<>();

        for (InterviewQuestion q : questions) {
            // 질문 ID 얻어서 답변 조회하기
            InterviewAnswer answer = interviewAnswerDao.selectInterviewAnswerByQuestionId(q.getQuestionId());

            // 질문-답변 조합 구성하기
            QuestionAnswerBundle questionAnswer = new QuestionAnswerBundle();
            questionAnswer.setQuestion(q.getQuestionText());
            questionAnswer.setAnswerText(answer != null ? answer.getAnswerText() : "");
            questionAnswer.setAnswerFeedback(answer != null ? answer.getAnswerFeedback() : "");

            qaList.add(questionAnswer);
        }

        if (qaList.isEmpty()) {
            throw new RuntimeException("No questions found for this session");
        }

        // 4. userPrompt에 전달할 질문/답변 문자열 구성하기
        StringBuilder qaBuilder = new StringBuilder();
        int idx = 1;
        for (QuestionAnswerBundle qa : qaList) {
            qaBuilder.append("Q").append(idx).append(": ").append(qa.getQuestion()).append("\n")
                     .append("A").append(idx).append(": ").append(qa.getAnswerText()).append("\n")
                     .append("피드백").append(idx).append(": ").append(qa.getAnswerFeedback()).append("\n\n");
            idx++;
        }
        // 사용자 조회
        Member member = memberDao.findById(session.getMemberId());

        // 5. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<SessionFeedbackResponse> converter = new BeanOutputConverter<>(SessionFeedbackResponse.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 6. 프롬프트 구성
        // 시스템
        String systemPrompt = """
            당신은 전문 취업 컨설턴트이며, 문장 분석·직무 역량 평가·논리적 흐름·면접관 관점의 평가에 능숙한 전문가입니다.
            톤은 따뜻하거나 무난한 조언이 아니라, 지원자를 성장시키기 위한 ‘냉정하고 직설적인 전문가 코멘트’ 스타일로 작성합니다.
            지나친 감정 표현, 공감 문구, 부드러운 표현은 절대 사용하지 않습니다.

            ※ 중요: speechAnalysis와 contentAnalysis는 절대 중복되지 않으며,
            speechAnalysis는 말투·표현 습관만, contentAnalysis는 답변의 내용·전문성만 평가합니다.
            speechAnalysis에서는 논리 구조나 전문성 언급을 절대 포함하지 않습니다.
            contentAnalysis에서는 발음, 말투, 억양, 템포와 같은 언어 습관 관련 표현을 절대 포함하지 않습니다.

            [평가 규칙]
            - 각 항목 점수는 0~100점 정수로 산정합니다.
            - 점수는 반드시 명확한 근거 기반으로 작성합니다.
            - 강점과 개선점은 실제 면접 대비에 활용 가능한 구체적 문장으로 작성합니다.
            - 개선 제안에는 반드시 ‘이후 행동(Action Plan)’이 포함되어야 합니다.
            - JSON 형식 이외 불필요한 문장 출력 금지.
            - 답변별 피드백에서 nonLanguageScore가 0이면서 "영상 정보가 없어"로 표시된 경우, 
            이는 비언어 평가 제외를 의미하며 종합 평가에서 감점 요소로 사용하지 않습니다.

            [세션 전체 점수 계산 규칙]
            1) languageScore 계산
            - 모든 답변의 languageScore 값을 수집한다.
            - 산술 평균을 구해 정수로 반올림한다.

            2) nonLanguageScore 계산
            - nonLanguageScore가 0이면서 "영상 정보가 없어" 라는 문구가 포함된 답변은 비언어 평균 계산에서 제외한다.
            - 제외되지 않은 답변들의 nonLanguageScore 값을 평균 내고 정수로 반올림한다.
            - 계산 대상이 하나도 없다면 nonLanguageScore = 0으로 설정한다.

            3) totalScore 계산
            - 각 답변(answerFeedback)의 totalScore 값을 모두 수집한다.
            - 이 값들의 산술 평균을 구하고 정수로 반올림한다.
            - 이 값이 세션의 최종 totalScore이다.

            [부적절한 입력 처리 규칙]
            다음 중 하나라도 포함된 답변은 분석에서 제외하며, 전체 평가에 영향을 주지 않도록 합니다:
            - 의미 없는 단어 나열(asdf, ㄱㄱㄱ, random text 등)
            - 문장 구조가 없는 단편적 단어
            - 욕설, 비속어, 공격적 표현
            - 답변이 비어 있거나 음성이 감지되지 않아 STT 텍스트가 존재하지 않는 경우
            - 답변별 피드백에서 ‘평가 불가’ 또는 ‘분석 불가’로 표시된 경우

            이 경우, 종합 분석에는 해당 문항을 제외하고 나머지 답변 기반으로 평가합니다.

            [출력 규칙]
            - 모든 문장은 공식 보고서 톤인 ‘~합니다’로 작성합니다.
            - 평서형 ‘~한다’는 절대 사용하지 않습니다.
            - 출력은 오직 JSON 형식으로만 작성합니다.
            - 아래 JSON 형식을 정확히 준수해 출력합니다.

            출력 형식:
            %s
        """.formatted(format);

        // 사용자
        String userPrompt = """

            아래는 면접 세션 전체 기록입니다.

            [면접 정보]
            - 면접 유형: %s
            - 희망 기업: %s
            - 지원자의 희망 직군: %s
            - 지원자의 희망 직무: %s

            [질문 및 답변 기록]
            %s

            위 데이터를 기반으로 면접 종합 피드백을 생성하세요.
        """.formatted(
            session.getInterviewType(),
            session.getTargetCompany(),
            member != null ? member.getJobGroup() : "정보 없음",    // 직군
            member != null ? member.getJobRole() : "정보 없음", // 직무
            qaBuilder.toString()
        );

        // 6. LLM 호출
        String json = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();

        // 7. JSON -> DTO 변환
        SessionFeedbackResponse feedback = converter.convert(json);

        // 8. DTO -> JSON으로 다시 직렬화해서 DB 업데이트
        String jsonString = objectMapper.writeValueAsString(feedback);
        session.setReportFeedback(jsonString);
        interviewSessionDao.updateInterviewFeedback(session);

        return feedback;
    }

}
