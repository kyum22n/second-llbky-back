package com.example.demo.ai.newstrend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.entity.CoverLetter;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.entity.InterviewSession;
import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.entity.Learning;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dao.SavedKeywordDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.GrowthAnalysisResponse;
import com.example.demo.newstrend.dto.response.InsightJson;
import com.example.demo.newstrend.entity.SavedKeyword;
import com.example.demo.newstrend.entity.TrendInsight;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.entity.Portfolio;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GrowthAnalysisAgent {
  private ChatClient chatClient;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private MemberDao memberDao;
  @Autowired
  private SavedKeywordDao savedKeywordDao;
  @Autowired
  private TrendInsightDao trendInsightDao;
  @Autowired
  private ResumeDao resumeDao;
  @Autowired
  private CoverLetterDao coverLetterDao;
  @Autowired
  private InterviewSessionDao interviewSessionDao;
  @Autowired
  private PortfolioDao portfolioDao;
  @Autowired
  private LearningDao learningDao;

  public GrowthAnalysisAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  /*
   * 성장 제안 에이전트
   */
  public GrowthAnalysisResponse generateGrowthAdvice(int memberId, String metaNews) throws Exception {
    // 사용자의 직무, 직군 정보 조회
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    // 저장 키워드
    List<SavedKeyword> keywords = savedKeywordDao.selectSavedKeywordListByMemberId(memberId);

    List<String> savedKeywords = new ArrayList<>();
    for (SavedKeyword sk : keywords) {
      savedKeywords.add(sk.getKeyword());
    }

    // 트렌드 분석 데이터
    InsightJson insightJson = null;
    TrendInsight trend = trendInsightDao.selectLatestTrendInsight(memberId);

    if (trend != null && trend.getInsightJson() != null) {
      insightJson = mapper.readValue(trend.getInsightJson(), InsightJson.class);
    }

    // 각 종합 피드백 추출
    List<Resume> resumes = resumeDao.selectResumesByMemberId(memberId);
    List<String> resumeFeedbackRaw = new ArrayList<>();
    for (Resume r : resumes) {
      resumeFeedbackRaw.add(r.getResumeFeedback()); // JSON 그대로
    }

    List<CoverLetter> coverLetters = coverLetterDao.selectAllCoverLetters(memberId);
    List<String> coverFeedbackRaw = new ArrayList<>();
    for (CoverLetter cl : coverLetters) {
      coverFeedbackRaw.add(cl.getCoverFeedback());
    }

    List<Portfolio> portfolios = portfolioDao.selectPortfoliosByMemberId(memberId);
    List<String> portfolioFeedbackRaw = new ArrayList<>();
    for (Portfolio p : portfolios) {
      portfolioFeedbackRaw.add(p.getPortfolioFeedback());
    }
    
    List<InterviewSession> sessions = interviewSessionDao.selectAllInterviewSessions(memberId);
    List<String> interviewReportsRaw = new ArrayList<>();
    for (InterviewSession s : sessions) {
      interviewReportsRaw.add(s.getReportFeedback());
    }

    List<Learning> learningList = learningDao.selectListByMemberId(memberId);
    List<String> learningRaw = new ArrayList<>();
    for(Learning l : learningList){
      learningRaw.add(l.getTitle()+" - "+ l.getStatus());
    }

    // 출력 변환기
    BeanOutputConverter<GrowthAnalysisResponse> converter = new BeanOutputConverter<>(GrowthAnalysisResponse.class);

    String format = converter.getFormat();

    // 프롬프트
    String systemPrompt = """
        이 기능은 '트렌드 기반 성장 제안'을 보여주는 기능이며,
        트렌드를 참고하되 근거 없이 특정 기술이 증가·감소한다고 단정하지 않아야 합니다.

        전체 원칙:
        1) 트렌드(insightJson)와 저장 키워드(savedKeywords)는 “참고 요소”로만 활용한다.
        2) 근거가 부족한 기술에 대해 “증가한다 / 급증한다 / 필수이다”와 같은 확정적 표현은 절대 사용하지 않는다.
        3) 대신 “대비해보세요 / 준비하면 좋습니다 / 강화해두면 도움이 됩니다” 같은 안전한 조언 문체를 사용한다.
        4) 이력서 · 면접 · 학습 조언 모두 ‘트렌드 흐름을 반영한 대비 방향성’을 중심으로 작성한다.
        5) 직무(jobRole)와 직접적으로 연관되지 않은 기술은 언급하지 않는다.
        6) 일반적인 조언이 되지 않도록, 트렌드나 사용자의 관심 키워드에서 파생된 “방향성”만 제시한다.
        7) metaNews는 뉴스 흐름 요약문으로 참고하여서 성장 제안을 생성해야 한다.(단, "기사에 따르면" 그대로 출력하거나 붙여넣기 금지)

        출력 스타일:
        - 확정 금지: “~가 증가하고 있습니다”, “~는 필수입니다”, “시장 수요가 급증하고 있습니다”
        - 권유/대비 중심: “~을 정리해두면 도움이 됩니다”, “~을 강화해두면 좋습니다”, “~에 대한 대비를 해보세요”

        카테고리 규칙:

        [이력서 조언]
        - 사용자의 희망 직무와 연관된 기술/키워드가 있다면 이를 활용해 프로젝트 경험을 보완하는 방향으로 작성.
        - “트렌드를 참고해 이런 부분을 정리해보면 좋다” 수준의 부드러운 조언 사용.

        [면접 조언]
        - 확정적 트렌드 언급 금지.
        - “최근 프레임워크 구조나 역할에 대한 질문 대비를 해보세요” 같이 안전한 방향성 중심 조언.
        - 저장 키워드나 직무와 자연스럽게 연결되는 대비 포인트 중심.

        [학습 조언]
        - 특정 기술의 수요 증가를 단정하지 않음.
        - 트렌드·관심 키워드를 참고해 “이 분야를 학습해두면 도움이 된다” 수준의 조언 제시.

        출력 형식:
        JSON only
        {
          "resumeAdvice": "...",
          "interviewAdvice": "...",
          "learningAdvice": "..."
        }
        각 항목은 1~2문장으로 작성.
        """.formatted(format);

    String request = """
        {
          "jobRole": %s,
          "jobGroup": %s,
          "savedKeywords": %s,
          "resume": %s,
          "coverLetter": %s,
          "interview": %s,
          "portfolio": %s,
          "learning": %s,
          "insightJson": %s,
          "metaNews": %s,
        }
        """.formatted(
        jobRole,
        jobGroup,
        mapper.writeValueAsString(savedKeywords),
        mapper.writeValueAsString(resumeFeedbackRaw),
        mapper.writeValueAsString(coverFeedbackRaw),
        mapper.writeValueAsString(interviewReportsRaw),
        mapper.writeValueAsString(portfolioFeedbackRaw),
        mapper.writeValueAsString(learningRaw),
        mapper.writeValueAsString(insightJson),
        metaNews);

    String response = chatClient.prompt()
        .system(systemPrompt)
        .user(request)
        .call()
        .content();

    // JSON -> DTO 변환
    return converter.convert(response);
  }

}
