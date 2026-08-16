package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.InsightJson;
import com.example.demo.newstrend.dto.response.JobInsightListResponse;
import com.example.demo.newstrend.entity.TrendInsight;
import com.fasterxml.jackson.databind.ObjectMapper;
/*
  직무 인사이트 에이전트
*/
@Component
public class JobRelatedInsightAgent {
  private ChatClient chatClient;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private TrendInsightDao trendInsightDao;
  @Autowired
  private MemberDao memberDao;

  public JobRelatedInsightAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public JobInsightListResponse relatedJobs(int memberId, String metaNews) throws Exception {

    // 사용자의 직무, 직군 정보 조회
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    // TrendInsight 조회
    TrendInsight trend = trendInsightDao.selectLatestTrendInsight(memberId);
    if (trend == null) {
      throw new RuntimeException("조회 실패");
    }
    // String -> DTO 변환
    InsightJson insightJson = mapper.readValue(trend.getInsightJson(), InsightJson.class);

    // 출력 변환기
    BeanOutputConverter<JobInsightListResponse> converter = new BeanOutputConverter<>(JobInsightListResponse.class);
    String format = converter.getFormat();

    // 프롬프트
    String systemPrompt = """
        당신은 'AI 직무 추천 인사이트 생성기'입니다.

        출력 규칙:
        1) JSON ONLY 출력
        2) BeanOutputConverter의 format 구조만 사용
        3) insights 배열은 반드시 3개 요소로 구성
        4) insights[0] = 사용자 직무(jobRole) 기반 추천
        5) insights[1], insights[2] = 사용자 직무와 연관성이 가장 높은 직무 2개를 LLM이 자동 판단해서 생성
        6) "연관성"의 기준:
           - 기술적으로 가장 가까운 분야
           - 커리어 확장성이 높은 분야
           - 현재 트렌드(InsightJson) 영향이 큰 분야
        7) null 금지, 빈 문자열 또는 빈 배열로 대체
        8) JSON 이외의 어떤 텍스트도 출력 금지
        9) metaNews는 뉴스 흐름 요약문으로 참고하여서 직무 인사이트를 생성해야 한다.(단, "기사에 따르면" 그대로 출력하거나 붙여넣기 금지)
        10) relatedKeywords 생성 규칙:
            - 직무 특성에 따라 키워드 유형을 판단한다.
            - 기술 중심 직무(개발, 데이터, 엔지니어)는:
              → 기술 / 도구 / 프레임워크 키워드를 포함할 수 있다.
            - 비기술 직무(기획, 마케팅, HR 등)는:
              → 산업 개념 / 업무 역량 / 트렌드 키워드를 우선한다.
            - 기술 키워드가 부적절한 직무에는 억지로 포함하지 않는다.

        아래 format JSON 구조를 그대로 따라 출력하세요:

        %s
        """.formatted(format);

    // 3) user prompt
    String userPrompt = """
        [사용자 직군(jobGroup)]
        %s

        [사용자 직무(jobRole)]
        %s

        [트렌드 분석 데이터 (InsightJson)]
        %s

        [뉴스 요약(metaNews)]
        %s

        요구사항:
        - 총 3개의 insights 항목을 생성하세요.
        - insights 배열의 첫 번째 항목은 반드시 사용자 직무(jobRole) 기반 인사이트여야 합니다.
        - insights 배열 총 길이는 3이어야 합니다.
        - summary는 jobRole(직무)에 대한 직업 설명으로 간단한 1문장으로 작성하세요.
        - trendSummary는 1~2문장으로 작성하세요.
        - relatedKeywords는 최소 3개 이상의 키워드를 포함한 문자열 배열이어야 합니다.
        - 출력은 반드시 JSON만 출력하고, format만 따라야 합니다.
        """.formatted(
        jobGroup,
        jobRole,
        mapper.writeValueAsString(insightJson),
        metaNews);

    String json = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();

    // JSON -> DTO 변환
    JobInsightListResponse response = converter.convert(json);

    return response;
  }
}
