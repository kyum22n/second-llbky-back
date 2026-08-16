package com.example.demo.ai.learning;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.response.RecommendSkillResponse;

// 직무 기반으로 부족 역량 추천해주는 Agent

@Component
public class RecommendSkillAgent {

  // ChatClient
  private ChatClient chatClient;

  public RecommendSkillAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public RecommendSkillResponse recommendSkillFromFeedback(String feedback) {

    // JSON 변환기
    BeanOutputConverter<RecommendSkillResponse> converter = new BeanOutputConverter<>(RecommendSkillResponse.class);
    String format = converter.getFormat();

    String system = """
        당신은 모든 직무(IT, 경영, 인사, 회계, 영업, 디자인, 생산·품질, 마케팅, 기획, 물류, 연구개발, 서비스업 등)를 아우르는
        전문 커리어 분석 AI입니다.

        당신의 역할:
        - 사용자의 서류(이력서, 자기소개서, 포트폴리오) 피드백을 기반으로 **지원자가 보완해야 할 정확한 능력 요소**를 추천합니다.
        - 추천은 반드시 **실제로 존재하는 기술·도구·업무 스킬·자격증·소프트스킬·방법론** 중 하나여야 합니다.

        ⚠️ 아래 내용은 절대 추천하지 마세요 (추상적 표현 금지):
        - “문서작성 능력”, “프로젝트 경험 부족”, “IT 역량”, “커뮤니케이션 능력",
        - “백엔드 개발 기술”, “자격증 취득”, “업무 이해도”, “문제 해결 능력”, “기획 역량”

        👍 추천 가능 항목 (예시는 직무별로 일부만 제시):
        - **IT 직무:** Java, Spring Boot, React, MySQL, Docker, AWS EC2, Git/GitHub, JPA, Linux
        - **회계/재무:** IFRS, 전산회계2급, FAT 1급, SAP FI 모듈, 더존 SmartA, 회계관리1급
        - **인사(HR):** HRD 기획, HR Analytics(Excel, SPSS), 직무기술서 작성법, 노무사 자격증, Workday 시스템
        - **마케팅/기획:** GA4, Adobe Analytics, 콘텐츠 기획법, Excel 분석 함수, PPT 고급 스킬, Notion, Figma
        - **디자인:** Photoshop, Illustrator, Figma, XD, 3D Blender, UI/UX 설계, Procreate
        - **영업:** CRM 사용 능력(Salesforce), 제안서 작성법, 엑셀 고객분석, 협상 스킬(명시적)
        - **생산/품질:** 6시그마 Green Belt, 공정관리, ISO9001, PLC, AutoCAD, Minitab
        - **물류:** SCM 시스템, SAP MM, 재고관리 기법, 물류관리사
        - **연구개발(R&D):** 실험기법명, 장비명, Python 분석, 설계도면 작성법, 전공별 실험기술

        💡 원칙:
        - 반드시 **구체적이고 실존하는 기술/자격증/방법론/도구명**을 추천하세요.
        - 추상적 능력이 아니라 “무엇을 공부해야 개선되는지”가 명확한 것이어야 합니다.
        - 1~6개 이내로 추천하세요.

        JSON 응답 형식:
        %s
        """.formatted(format);

    String prompt = """
        다음은 사용자의 이력서, 자기소개서, 포트폴리오에서 추출된 AI 피드백입니다.
        이 피드백을 분석하여 지원자가 부족한 **실존하는 기술명, 자격증, 도구, 분석기법, 업무스킬**만 추천하세요.

        지원 직무는 특정되지 않았으며, 모든 직무에서 추론 가능합니다.

        피드백:
        ----------------------
        %s
        ----------------------
        """.formatted(feedback);

    String json = chatClient.prompt()
        .system(system)
        .user(prompt)
        .call()
        .content();

    // JSON -> DTO 변환
    RecommendSkillResponse result = converter.convert(json);

    return result;
  }

}
