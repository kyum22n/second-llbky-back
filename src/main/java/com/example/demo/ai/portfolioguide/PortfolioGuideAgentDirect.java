package com.example.demo.ai.portfolioguide;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.request.GuideRequest;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PortfolioGuideAgentDirect {
  private ChatClient chatClient;

  public PortfolioGuideAgentDirect(ChatClient.Builder chatClientBuilder, MemberDao memberDao) {
    this.chatClient = chatClientBuilder
        .defaultSystem("""
              당신은 포트폴리오 작성을 도와주는 전문적인 AI 코치입니다.

            """)
        .build();
  }

  public GuideResult evaluateDirect(GuideRequest request, Member member) {
    LocalDateTime startTime = LocalDateTime.now();

    BeanOutputConverter<GuideResult> converter = new BeanOutputConverter<>(GuideResult.class);
    String format = converter.getFormat();

    // 1. 회원 정보 추출
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();
    Integer careerYears = member.getCareerYears();

    LocalDateTime guidelineStart = LocalDateTime.now();
    String standardsGuidelines = getHardcodedGuidelines();
    long promptGenerationTime = Duration.between(guidelineStart, LocalDateTime.now()).toMillis();

    // 3. 프롬프트 구성 (DB 방식과 동일)
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
        jobGroup, jobRole, careerYears,
        request.getInputFieldType(),
        request.getCurrentStep(),
        request.getUserInput() != null ? request.getUserInput() : "",
        standardsGuidelines,
        format);

    LocalDateTime llmCallStart = LocalDateTime.now();
    ChatResponse chatResponse = chatClient.prompt()
        .user(prompt)
        .call()
        .chatResponse();

    String json = chatResponse.getResult().getOutput().getText();
    long llmCallTime = Duration.between(llmCallStart, LocalDateTime.now()).toMillis();

    LocalDateTime endTime = LocalDateTime.now();
    Duration processingTime = Duration.between(startTime, endTime);
  



    // 5. 토큰 사용량 로그
    if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
      var usage = chatResponse.getMetadata().getUsage();
      int promptCharLength = prompt.length();


      log.info("=== 토큰 사용량 (Direct 하드코딩 방식) ===");
      log.info("입력 필드: {}, 현재 단계: {}", request.getInputFieldType(), request.getCurrentStep());
      log.info("입력 토큰: {} tokens (프롬프트 길이: {} chars, 비율: ~{} chars/token)",
          usage.getPromptTokens(), promptCharLength,
          promptCharLength / Math.max(usage.getPromptTokens(), 1));
      log.info("출력토큰:{} tokens",usage.getCompletionTokens());
      log.info("총 토큰: {} tokens", usage.getTotalTokens());
      log.info("DB 조회: 없음 (하드코딩)");
      log.info("===============================");

      log.info("=== 성능 지표 (Direct) ===");
      log.info("전체 처리 시간: {} ms", processingTime.toMillis());
      log.info("프롬프트 생성 시간: {} ms", promptGenerationTime);
      log.info("LLM 호출 시간: {} ms", llmCallTime);
      log.info("DB 조회 시간: 0 ms (없음)");
      log.info("================");



      log.info(" CSV: Direct,{},{},{},{},{},{}",
      processingTime,
      usage.getPromptTokens(),
      usage.getCompletionTokens()
    ,usage.getTotalTokens()
    ,promptCharLength);
  
    

    }

    GuideResult result = converter.convert(json);


    return result;

  }

  private String getHardcodedGuidelines() {
    return """
        ### 프로젝트 개요
              프로젝트의 목적, 배경, 주요 목표를 명확히 설명해야 합니다.

              **평가 항목:**
              - 프로젝트 제목의 명확성
              - 개발 기간 및 참여 인원
              - 프로젝트 목적 및 배경 설명
              - 주요 기술 스택 나열
              - 담당 역할 및 기여도

              ### 기술 스택
              사용한 기술의 적절성과 깊이를 평가합니다.

              **평가 항목:**
              - 기술 선택의 적절성
              - 기술 스택의 최신성
              - 기술 간 조합의 합리성
              - 새로운 기술 학습 의지

              ### 핵심 성과
              프로젝트의 결과와 임팩트를 평가합니다.

              **평가 항목:**
              - 정량적 성과 제시
              - 문제 해결 과정
              - 비즈니스 임팩트
              - 기술적 난이도

              ### 코드 품질
              코드의 완성도와 설계를 평가합니다.

              **평가 항목:**
              - 코드 구조 및 설계
              - 테스트 코드 작성
              - 코드 리뷰 경험
              - 리팩토링 경험

              ### 협업 능력
              팀 협업 경험을 평가합니다.

              **평가 항목:**
              - 팀 프로젝트 경험
              - 커뮤니케이션 능력
              - 코드 리뷰 참여
              - 문서화 수준
        """;
  }

}
