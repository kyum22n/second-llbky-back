package com.example.demo.ai.portfolioguide;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.request.GuideRequest;
import com.example.demo.portfolio.entity.PortfolioStandard;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 가이드 코칭을 담당하는 에이전트
 * - 자동으로 MemberDao에서 회원 정보 조회
 * - 자동으로 PortfolioStandardDao에서 직무별 표준 가이드라인 조회
 * - LLM을 이용하여 실시간 코칭 제공
 */
@Component
@Slf4j
public class PortfolioGuideAgent {

    private ChatClient chatClient;

    public PortfolioGuideAgent(
            ChatClient.Builder chatClientBuilder,
            MemberDao memberDao,
            PortfolioStandardDao portfolioStandardDao) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        당신은 포트폴리오 작성을 도와주는 전문적인 AI 코치입니다.

                        다음 원칙을 따라 답변해주세요:
                        1. 전문적이고 냉정한 톤으로 대화하세요
                        2. 구체적이고 실행 가능한 조언을 제공하세요
                        3. 사용자의 경력 수준에 맞는 적절한 피드백을 주세요
                        4. 항상 JSON 형식으로 구조화된 응답을 제공하세요
                        5. 긍정적인 측면을 먼저 언급한 후 개선점을 제시하세요

                        당신의 목표는 사용자가 자신만의 강점을 잘 표현할 수 있도록 돕는 것입니다.

                         부적절한 입력 처리 규칙:
                        만약 사용자 입력이 다음 중 하나라도 해당하면 “평가 불가”로 처리해야 합니다:
                        - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
                        - 문장 구조가 없는 단편적 단어
                        - 욕설, 비속어, 공격적 표현
                        - 자소서 항목으로 볼 수 없는 내용
                        - 항목 전체가 비어 있거나 공란인 경우
                        """)
                .build();

    }

    /*
     * 순수 AI 평가 메서드
     */
    public GuideResult evaluate(GuideRequest request,
            Member member,
            List<PortfolioStandard> standards) throws Exception {

        // 0. 성능 측정 시작
        LocalDateTime startTime = LocalDateTime.now();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        BeanOutputConverter<GuideResult> converter = new BeanOutputConverter<>(GuideResult.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 2. 회원 정보 추출 (Member에서만)
        String jobGroup = member.getJobGroup();
        String jobRole = member.getJobRole();
        Integer careerYears = member.getCareerYears();

        // 3. 표준 가이드라인 구성
        LocalDateTime guidelineStart = LocalDateTime.now();
        String standardsGuidelines = buildStandardsGuidelines(standards);
        long promptGenerationTime = Duration.between(guidelineStart, LocalDateTime.now()).toMillis();

        // 4. 프롬프트 구성 - String.formatted() 방식
        String prompt = """
                # 포트폴리오 가이드 실시간 코칭

                사용자가 입력하는 내용을 실시간으로 분석하여 직군/직무 기준에 맞는 구체적인 피드백을 제공하세요.

                ## 사용자 정보
                - 직군: %s
                - 직무: %s
                - 경력: %d년

                ## 현재 작성 중인 항목
                - 입력 필드: %s
                - 현재 단계: %d단계
                - 사용자 입력 내용:
                "%s"

                ## 직무별 표준 가이드라인 및 평가 지침
                %s

                ## 코칭 요구사항

                다음 내용을 반드시 포함하여 피드백을 제공하세요:

                1. **coachingMessage (코칭 메시지)**:
                   - 현재 작성 내용의 강점을 먼저 칭찬 (1-2문장)
                   - 부족한 부분 또는 개선이 필요한 이유 설명 (2-3문장)
                   - 격려와 함께 마무리

                3. **suggestions (개선 제안)**:
                   - 현재 입력을 기준으로 구체적으로 어떻게 수정/추가할지 3-5개 제안
                   - 각 제안은 실행 가능하고 명확해야 함
                   - 예: "프로젝트 기간을 '2024.03 ~ 2024.06 (3개월)' 형식으로 명시하세요"

                4. **examples (작성 예시)**:
                   ⭐ **매우 중요** - 사용자의 현재 입력 내용을 토대로 개선된 버전을 2-3개 제시
                   - 사용자가 입력한 핵심 키워드/내용을 유지하면서 더 구체적으로 작성
                   - 단순한 일반 예시가 아닌, 사용자 입력의 "업그레이드 버전"이어야 함
                   - 각 예시는 서로 다른 스타일로 작성 (간결형, 상세형, 기술 중심형 등)

                   예시 생성 가이드:
                   - 사용자 입력: "여행 추천 앱"
                   - 잘못된 예시: "쇼핑몰 프로젝트", "날씨 앱" (전혀 다른 주제)
                   - 올바른 예시 1: "개인 맞춤형 여행 추천 서비스 - 사용자 선호도 기반 AI 추천 시스템"
                   - 올바른 예시 2: "여행지 추천 및 일정 자동 생성 플랫폼 (Spring Boot + ChatGPT API)"
                   - 올바른 예시 3: "빅데이터 분석 기반 여행지 추천 앱 - 월 10만 사용자 대상"

                5. **nextStepGuide (다음 단계)**: 현재 항목을 완성한 후 다음에 작성할 내용 안내

                6. **progressPercentage (진행률)**: 현재 입력 완성도를 0-100%%로 평가

                ## 출력 형식 (엄격한 JSON)

                반드시 아래 JSON 형식을 그대로 채워서 출력해라:
                %s

                **주의**:
                - JSON 형식을 정확히 지켜주세요. 다른 텍스트는 포함하지 마세요.
                - examples는 사용자의 현재 입력을 기반으로 개선한 버전이어야 합니다!
                """.formatted(
                jobGroup,
                jobRole,
                careerYears,
                request.getInputFieldType(),
                request.getCurrentStep(),
                request.getUserInput() != null ? request.getUserInput() : "",
                standardsGuidelines,
                format);

        // 5. LLM 호출 및 토큰 사용량 추적
        LocalDateTime llmCallStart = LocalDateTime.now();
        ChatResponse chatResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String json = chatResponse.getResult().getOutput().getText();
        long llmCallTime = Duration.between(llmCallStart, LocalDateTime.now()).toMillis();
        LocalDateTime endTime = LocalDateTime.now();
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        Duration processingTime = Duration.between(startTime, endTime);
        long memoryUsed = endMemory - startMemory;

        // 토큰 사용량 로그 출력 및 프롬프트 길이 분석
        if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
            var usage = chatResponse.getMetadata().getUsage();
            int promptCharLength = prompt.length();
            log.info("=== 토큰 사용량 (DB 기반 실시간 코칭) ===");
            log.info("입력 필드: {}, 현재 단계: {}", request.getInputFieldType(), request.getCurrentStep());
            log.info("입력 토큰: {} tokens (프롬프트 길이: {} chars, 비율: ~{} chars/token)",
                    usage.getPromptTokens(), promptCharLength,
                    promptCharLength / Math.max(usage.getPromptTokens(), 1));

            log.info("총 토큰: {} tokens", usage.getTotalTokens());
            log.info("DB에서 조회한 평가기준 개수: {} 개", standards != null ? standards.size() : 0);
            log.info("===============================");

            // 5. 성능 지표 로그 출력
            log.info("=== 성능 지표 ===");
            log.info("전체 처리 시간: {} ms", processingTime.toMillis());
            log.info("프롬프트 생성 시간: {} ms", promptGenerationTime);
            log.info("LLM 호출 시간: {} ms", llmCallTime);
            log.info("메모리 사용량: {} bytes (~{} KB)", memoryUsed, memoryUsed / 1024);
            log.info("평가기준 개수: {} 개", standards != null ? standards.size() : 0);
            log.info("================");

            log.info("CSV: DB,{},{},{},{},{},{}", 
            processingTime,
            usage.getPromptTokens(),
            usage.getCompletionTokens(), 
            usage.getTotalTokens(),
            promptCharLength,
            standards != null ? standards.size() : 0);


        }

        // 6. JSON -> DTO 변환
        GuideResult result = converter.convert(json);

        

        return result;

    }

    /**
     * 표준 가이드라인 텍스트 구성
     */
    private String buildStandardsGuidelines(List<PortfolioStandard> standards) {
        if (standards == null || standards.isEmpty()) {

            return """
                    ## 일반 평가 지침

                    입력된 내용을 다음 기준으로 평가하세요:
                    - 전체 적절성 점수 (1-10점)
                    - 구체적인 개선 제안 3가지
                    - 해당 직무에 적합한 예시 2-3개
                    - 다음 단계 작성 가이드
                    """;
        }

        StringBuilder sb = new StringBuilder();
        for (PortfolioStandard standard : standards) {
            sb.append(String.format("### %s\n", standard.getStandardName()));
            sb.append(String.format("%s\n\n", standard.getStandardDescription()));

            if (standard.getPromptTemplate() != null &&
                    !standard.getPromptTemplate().isEmpty()) {
                sb.append(standard.getPromptTemplate());
                sb.append("\n\n");
            }

            // PortfolioStandard의 evaluationItems 활용
            if (standard.getEvaluationItems() != null) {
                sb.append("**평가 항목:**\n");
                sb.append(standard.getEvaluationItems());
                sb.append("\n\n");
            }

        }
        return sb.toString();
    }

}
