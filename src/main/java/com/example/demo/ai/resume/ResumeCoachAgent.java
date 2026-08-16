package com.example.demo.ai.resume;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.resume.dto.request.ResumeCoachRequest;
import com.example.demo.resume.dto.response.ResumeCoachResponse;

import lombok.extern.slf4j.Slf4j;

/*
  이력서 실시간 코칭
*/
@Component
@Slf4j
public class ResumeCoachAgent {
  private ChatClient chatClient;
  @Autowired
  private MemberDao memberDao;

  public ResumeCoachAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public ResumeCoachResponse coach(ResumeCoachRequest request) {
    // 사용자의 직무, 직군 정보 조회
    Member member = memberDao.findById(request.getMemberId());
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    // 출력 변환기 생성
    BeanOutputConverter<ResumeCoachResponse> converter = new BeanOutputConverter<>(ResumeCoachResponse.class);

    String format = converter.getFormat();

    // 경력/활동 구분
    String sectionDetail = "";

    if ("career".equals(request.getSection())) {
      sectionDetail = """
          [경력 정보]
          - 회사명: %s
          - 직무: %s
          - 입사일: %s
          - 퇴사일: %s
          - 현재 재직중: %s
          """.formatted(
          request.getCompany(),
          request.getPosition(),
          request.getStartDate(),
          request.getEndDate(),
          request.getIsCurrent());
    }

    if ("activity".equals(request.getSection())) {
      sectionDetail = """
          [활동 정보]
          - 활동명: %s
          - 기관/단체: %s
          - 시작일: %s
          - 종료일: %s
          """.formatted(
          request.getActivityName(),
          request.getOrganization(),
          request.getActivityStart(),
          request.getActivityEnd());
    }

    // 프롬프트
    String systemPrompt = """
        당신은 취업 준비생에게 이력서를 첨삭해주는 전문 취업 컨설턴트입니다.

        출력 규칙:
        - 반드시 JSON만 출력
        - 제공된 format 구조 외의 필드 추가 금지
        - improvedText는 사용자의 문장을 자연스럽고 전문적으로 수정한 버전으로 작성
        - 모든 문장은 공식 보고서 톤인 ‘~합니다’ 형태로 작성합니다.
        - ‘~한다’ 형태의 평서형은 금지합니다.
        - 한국어로 작성하세요.

        키워드 반영 규칙:
        - 사용자가 선택한 키워드는 improvedText에 반드시 자연스럽게 녹여서 포함해야 합니다.
        - 단, 명백히 문맥과 상충되는 경우에는 강제로 포함하지 말고 '강점 요약' 또는 '개선점'에서 간접적으로 반영해도 됩니다.
        - 키워드는 기술 스택, 성과, 사용 도구, 업무 맥락을 보완하는 방식으로 활용하세요.

        부적절한 입력 처리 규칙:
          만약 사용자 입력이 다음 중 하나라도 해당하면 “평가 불가”로 처리해야 합니다:
          - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
          - 문장 구조가 없는 단편적 단어
          - 욕설, 비속어, 공격적 표현
          - 이력서 항목으로 볼 수 없는 내용
          - 항목 전체가 비어 있거나 공란인 경우

          아래 JSON format에 맞게 출력하세요:
          %s
        """.formatted(format);

    String prompt = """
        사용자 섹션: %s

        %s   // ← 경력 또는 활동 정보 자동 삽입

        [입력 내용]
        %s

        [선택된 키워드]
        %s

        [지원자 정보]
        - 희망 직군: %s
        - 희망 직무: %s

        이 정보를 기반으로 해당 직무에서 중요하게 평가되는 요소 중심으로 코칭하세요.
        """.formatted(
        request.getSection(),
        sectionDetail,
        request.getContent(),
        request.getKeywords(),
        jobGroup,
        jobRole);

    log.info("받은 키워드: {}", request.getKeywords());

    String json = chatClient.prompt()
        .system(systemPrompt)
        .user(prompt)
        .call()
        .content();

    // JSON -> DTO 변환
    ResumeCoachResponse result = converter.convert(json);

    return result;
  }
}
