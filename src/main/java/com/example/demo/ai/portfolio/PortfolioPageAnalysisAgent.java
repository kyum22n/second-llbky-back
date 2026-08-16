package com.example.demo.ai.portfolio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.response.PortfolioPageFeedbackResponse;
import com.example.demo.portfolio.entity.PortfolioImage;
import com.example.demo.portfolio.entity.PortfolioStandard;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 페이지별로 분석하는 Agent
*/

@Component
@Slf4j
public class PortfolioPageAnalysisAgent {

  // 필드
  private ChatClient chatClient;

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private PortfolioStandardDao portfolioStandardDao;

  @Autowired
  private MemberDao memberDao;

  // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
  @Autowired
  private ObjectMapper objectMapper;

  // 생성자
  public PortfolioPageAnalysisAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // Agent
  public List<PortfolioPageFeedbackResponse> analyzePortfolio(byte[] pdfBytes, Integer portfolioId, Integer memberId)
      throws Exception {

    // Bean 객체 -> JSON 출력 변환기 생성
    BeanOutputConverter<PortfolioPageFeedbackResponse> converter = new BeanOutputConverter<>(PortfolioPageFeedbackResponse.class);

    // DTO 구조 제공 -> JSON 출력 포맷 지정
    String format = converter.getFormat();

    PDDocument document = PDDocument.load(pdfBytes); // PDFBox를 이용해 PDF 파일을 로드
    PDFRenderer renderer = new PDFRenderer(document); // PDF 페이지를 하나씩 이미지를 그릴 수 있는 도구 생성
    int pageCount = document.getNumberOfPages(); // PDF 페이지 수 가져오기
    portfolioDao.updatePortfolioPageCount(portfolioId, pageCount);

    List<PortfolioPageFeedbackResponse> feedbackList = new ArrayList<>(); // 피드백 결과를 반환하기 위한 리스트 생성

    // 사용자의 직무, 직군을 불러와서 포트폴리오 프롬프트에 전달
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();
    
    List<PortfolioStandard> standards = portfolioStandardDao.selectStandardsByJobInfo(jobGroup, jobRole);
    String standardsGuidelines = buildStandardsGuidelines(standards);

    for (int i = 0; i < pageCount; i++) {

      // PDF 페이지를 이미지로 렌더링
      // dpi: 이미지 해상도를 결정하는 숫자. 200-250이 이미지 변환 시 가장 많이 사용
      BufferedImage image = renderer.renderImageWithDPI(i, 200); // 토큰 제한으로 최소 dpi로 최대 효율을 낼 수 있는 96으로 수정

      // PNG 형식 byte[] 로 변환
      // 파일로 저장하면 다시 읽어서 byte[]로 변환 → 비효율적. 메모리에서 바로 PNG 변환 후 byte[] 얻기
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); // 출력 메모리 버퍼
      ImageIO.write(image, "png", baos); // 이미지 파일 저장 기능

      byte[] imageBytes = baos.toByteArray(); // baos 내부의 메모리 byte들을 실제 바이트 배열로 꺼내기
      int pageNo = i + 1;

      // SystemMessage 생성
      SystemMessage systemMessage = SystemMessage.builder()
          .text("""
                당신은 다양한 직군의 포트폴리오를 평가하는 전문 분석가입니다. 
                분석 시 반드시 아래 네 가지 영역을 구분하여 평가하십시오:

                1) 시각적 구성 분석 (이미지 기반)
                - 요소 배치, 여백, 정렬, 비율, 컬러 대비, 시각적 계층 구조
                - 읽기 흐름이 자연스러운지, 강조 포인트가 명확한지
                - 시각적 방해 요소 또는 과도한 장식의 존재 여부

                2) 콘텐츠 분석 (텍스트와 정보 구조 기반)
                - 페이지가 전달하려는 메시지가 무엇인지 추론하고 그 명확성을 평가
                - 정보가 충분한지, 부족한지, 불필요하게 과한지
                - 설명이 논리적으로 연결되는지, 핵심 내용이 제대로 드러나는지
                - 포함하면 좋을 내용과 제거하면 좋은 내용을 구분하여 제안

                3) 직무 적합성 분석 (모든 직군 공통)
                - 해당 페이지 내용이 지원 직군·직무와 얼마나 관련성이 있는지 평가
                - 직무 특성(기획/디자인/마케팅/개발/영업/HR/연구 등)에 필요한 핵심 정보가 포함되었는지
                - 불필요하게 강조된 내용 또는 빠져 있는 중요한 요소가 무엇인지 지적
                - 실제 산업 현장에서 포트폴리오 평가 기준에 맞는지 판단

                4) 개선 방향 제안 (실행 가능한 형태)
                - 디자인, 콘텐츠, 구조적 문제를 구체적인 근거를 기반으로 수정 제안
                - 단순한 추상적 조언이 아니라 실현 가능한 형태로 작성
                  예: “우측 상단 통계 그래프의 숫자 대비가 낮아 가독성이 떨어집니다. 배경을 어둡게 하거나 폰트 굵기를 높이면 개선됩니다.”

                금지 규칙:
                - 모호하고 일반적인 표현 금지 (“전반적으로 부족함”, “내용이 약함”, “구성이 아쉬움” 등)
                - 이미지에 없는 내용을 추측하거나 생성하지 말 것
                - 감정적·칭찬 위주의 문구 금지

                출력은 반드시 아래 JSON format과 일치해야 하며, JSON 문자열로 출력하세요:
                %s

                출력 예시:
                {
                  "pageSummary": "이 페이지 전체 내용을 한 문단으로 요약",
                  "pageComment": "분석 및 코멘트 작성"
                }

              """.formatted(format))
          .build();

      // 이미지 bytes -> Resource
      ByteArrayResource resource = new ByteArrayResource(imageBytes) {
        @Override
        public String getFilename() {
          return "page.png"; // LLM에게 보낼 이미지 이름
        }
      };

      // Resource -> Media 객체로 변환
      Media media = Media.builder() // LLM에게 보낼 수 있는 멀티모달 데이터 패키지
          .mimeType(MimeType.valueOf("image/png"))
          .data(resource)
          .build();

      // userMessage 생성
      UserMessage userMessage = UserMessage.builder()
          .text("""
              다음은 포트폴리오의 %d번 페이지 이미지입니다.
              이 페이지를 분석하여 format에 맞는 JSON만 출력하십시오.

              지원자 정보:
              - 직군: %s
              - 역할: %s

              아래 기준을 만족하며 이번 페이지에 대한 *새로운* 분석을 생성하세요.
              중복된 조언, 동일 문장 패턴, 같은 논조의 피드백을 생성하지 마십시오.

              분석 시 반드시 다음을 수행하십시오:

              1) 페이지 목적을 이미지 및 텍스트 기반으로 추론하십시오.
              2) 시각적 요소(배치, 여백, 정렬, 색상 등)와 콘텐츠 요소(설명, 정보 구조)를 분리해 평가하십시오.
              3) 해당 직군의 실제 포트폴리오 평가 기준에 따라 직무 적합성을 판단하십시오.
              4) 포함하면 좋을 콘텐츠, 제거하면 좋을 콘텐츠를 명확히 제시하십시오.
              5) 문제점을 반드시 ‘실제 요소를 근거로’ 구체적으로 설명하십시오.
              6) 개선 포인트는 실행 가능해야 하며 단순한 조언이 아니라 구체적 조치로 작성하십시오.

              ## 직무별 표준 가이드라인 및 평가 지침
              %s

              반드시 JSON만 출력하십시오.
""".formatted(pageNo, jobGroup, jobRole, pageNo, standardsGuidelines))
          .media(media)
          .build();

      // LLM 호출
      String feedback = chatClient.prompt()
          .messages(systemMessage, userMessage)
          .call()
          .content();

      // JSON -> DTO 변환
      PortfolioPageFeedbackResponse response = converter.convert(feedback);
      log.info("페이지 {} 분석 결과: {}", pageNo, response);

      // DTO -> JSON으로 다시 직렬화해서 DB 업데이트
      String responseJson = objectMapper.writeValueAsString(response);

      // DB 저장
      PortfolioImage portfolioImage = new PortfolioImage();
      portfolioImage.setPortfolioId(portfolioId);
      portfolioImage.setPageNo(pageNo);
      portfolioImage.setPageFeedback(responseJson);
      portfolioImageDao.insertPortfolioImage(portfolioImage);

      feedbackList.add(response);

      // 5초간 sleep
      Thread.sleep(5000);
    }

    document.close(); // 메모리 누수 방지
    return feedbackList;
  }


  /**
   * 표준 가이드라인 텍스트 구성
   */
  private String buildStandardsGuidelines(List<PortfolioStandard> standards) {

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
