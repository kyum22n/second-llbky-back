package com.example.demo.ai.portfolio;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dto.response.PortfolioSummaryResponse;
import com.example.demo.portfolio.entity.PortfolioImage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 종합 피드백 Agent
*/

@Component
@Slf4j
public class PortfolioSummaryAnalysisAgent {

  // 필드
  private ChatClient chatClient;

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private MemberDao memberDao;

  // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
  @Autowired
  private ObjectMapper objectMapper;

  // 생성자
  public PortfolioSummaryAnalysisAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public PortfolioSummaryResponse generateSummary(Integer portfolioId, Integer memberId) throws Exception {

    // Bean 객체 -> JSON 출력 변환기 생성
    BeanOutputConverter<PortfolioSummaryResponse> converter = new BeanOutputConverter<>(PortfolioSummaryResponse.class);

    // DTO 구조 제공 -> JSON 출력 포맷 지정
    String format = converter.getFormat();

    // 사용자 직군 직무 조회
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    // 페이지별 피드백 전체 조회
    List<PortfolioImage> pageFeedbackList = portfolioImageDao.selectImagesByPortfolioId(portfolioId);

    // 시스템 메시지
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
                당신은 전문 취업 컨설턴트이며 포트폴리오 전문 평가자입니다.
                톤은 따뜻하거나 무난한 조언이 아니라, 지원자를 성장시키기 위한 ‘냉정하고 직설적인 전문가 코멘트’ 스타일로 답변해야 합니다.
                지나친 감정 표현, 공감 문구, 부드러운 문장 사용을 절대 포함하지 않습니다.

                [출력 규칙 — 매우 중요]
                1. finalScore만 0~100 범위의 정수 점수로 작성합니다.
                2. 나머지 모든 항목(strengths, weaknesses, expression, visualDesign, contentQuality,
                  informationStructure, technicalComposition, overallReview)에는 **숫자를 절대 포함하지 않습니다.**
                3. 세부 항목은 **설명형 문장** 또는 **문장 배열**만 작성합니다.
                4. 점수, 수치, 등급 표현(A~F 등)도 금지합니다.
                5. JSON 구조 외 텍스트는 절대 출력하지 않습니다.
                6. 각 항목은 구체적이며 근거 기반으로 작성합니다. 추상적 표현, 반복 표현 금지.

                부적절한 입력 처리 규칙:
                입력 이미지 또는 텍스트가 다음 중 하나라도 해당하면 “평가 불가”로 처리합니다:
                - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
                - 의미 없는 이미지, 깨진 페이지, 내용 식별 불가
                - 욕설, 비속어, 공격적 표현
                - 항목 전체가 비어 있거나 분석할 수 있는 정보가 없는 경우

                모든 문장은 공식 보고서 톤인 ‘~합니다’ 형태로 작성하며, ‘~한다’와 같은 평서형은 절대 사용하지 않습니다.

                아래 JSON 형식을 정확히 준수해 답변할 것:
                %s
            """.formatted(format))
        .build();

    // 사용자 메시지
    UserMessage userMessage = UserMessage.builder()
        .text("""
            다음은 포트폴리오 각 페이지의 분석 요약 리스트입니다.
            이를 종합하여 전체 분석 JSON을 생성합니다.

            [참고 정보]
            - 직군: %s
            - 역할: %s

            페이지 피드백 리스트:
            %s

            아래 요구사항에 따라 종합 분석 JSON을 작성합니다:
            - 전체적인 디자인, 메시지 전달력, 정보 구조, 기술적 표현, 콘텐츠 품질을 평가합니다.
            - 강점/약점은 구체적인 문장으로 작성합니다.
            - 점수는 0~100점 사이 정수로 계산합니다.
            - overallReview는 3~5문장으로 작성합니다.

            반드시 format에 있는 JSON key만 사용해 정확히 JSON만 출력합니다.
        """.formatted(jobGroup, jobRole, pageFeedbackList.toString()))
        .build();

    // LLM 호출
    String summary = chatClient.prompt()
        .messages(systemMessage, userMessage)
        .call()
        .content();

    PortfolioSummaryResponse response = converter.convert(summary);
    log.info("최종 포트폴리오 분석 결과: {}", response);

    // DTO -> JSON으로 다시 직렬화해서 DB 업데이트
    String summaryJson = objectMapper.writeValueAsString(response);
    portfolioDao.updatePortfolioFeedback(portfolioId, summaryJson);

    return response;
  }

}
