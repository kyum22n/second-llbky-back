package com.example.demo.ai.newstrend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.dto.response.TrendDataContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/*
  TrendAnalysisAgent
  - LLM이 분석만 수행하는 에이전트
  rawTrendData(트렌드 데이터 수집본)을 기반으로 계산 및 요약 분석 수행
*/
@Component
@Slf4j
public class TrendAnalysisAgent {
  private ChatClient chatClient;
  @Autowired
  private ObjectMapper mapper;

  public TrendAnalysisAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao,
      MemberDao memberDao) {
    this.chatClient = chatClientBuilder.build();
  }

  public TrendAnalyzeResponse analyze(TrendDataContext context) throws Exception {

    /*
     * 백엔드 계산 단계
     * 1) counts
     * - 각 키워드별 "평균 관심도"
     * - Naver 트렌드 ratio 값들의 산술 평균
     *
     * 2) changeRate (C 값)
     * - 최근 관심도 변화 추이를 나타내는 지표
     * - 초반 3일 평균 대비 후반 3일 평균 비율
     *
     * 3) avgInterest
     * - 전체 키워드의 평균 관심도
     */

    // 트렌드 분석 대상 키워드 목록 조회
    List<String> keywords = context.getKeywords();

    // 키워드별 원본 트렌드 데이터(Map 형태) 조회
    Map<String, Object> rawTrendData = context.getRawTrendData();

    // 키워드별 평균 관심도(counts)를 담을 리스트
    List<Integer> counts = new ArrayList<>();

    // 키워드별 관심도 변화 지표(C 값)를 담을 맵
    Map<String, Double> changeRateMap = new HashMap<>();

    // 모든 키워드를 순회하며 트렌드 지표 계산
    for (String keyword : keywords) {

      // 현재 키워드에 해당하는 원본 트렌드 데이터 추출
      Object raw = rawTrendData.get(keyword);

      // 원본 데이터에서 ratio 값 리스트 추출
      List<Integer> ratios = extractRatios(raw);

      // ratio 평균을 계산하여 키워드별 평균 관심도로 추가
      counts.add(calculateAverage(ratios));

      // ratio 변화 추이를 계산하여 키워드별 변화 지표로 저장
      changeRateMap.put(keyword, calculateChangeRate(ratios));
    }

    // 모든 키워드의 평균 관심도를 기반으로 전체 평균 관심도 계산
    double avgInterest = calculateAvgInterest(counts);

    String systemPrompt = """
        너는 채용/기술 트렌드를 분석하는 TrendAnalysisAgent이다.

        입력은 TrendDataContext(JSON)이며,
        추가로 백엔드에서 계산된 지표들이 함께 제공된다.

        ==================================================
        ■ TrendAnalyzeResponse — 반드시 아래 구조 동일하게 출력
        ==================================================
        {
          "trendJson": {
            "keywords": ["string"],
            "counts": [number],
            "rawTrendData": {
              "<keyword>": {
                ... 원본 그대로 ...
              }
            }
          },

          "insightJson": {
            "summarycard": {
              "majorKeyword": "string",
              "avgInterest": number,
              "keywordCount": number
            },

            "industrySentiment": [
              { "industry": "string", "positive": number, "neutral": number, "negative": number }
            ],

            "wordCloud": [
              { "keyword": "string", "score": number }
            ],

            "marketInsight": ["string", "string", "string"],
            "finalSummary": "string"
          }
        }

        ────────────────────────────────────
        ■ 역할 정의 (절대 위반 금지)
        ────────────────────────────────────
        - 너는 "계산"을 하지 않는다.
        - 산술 평균, 반올림, 비율 계산, 지표 계산은
          이미 백엔드에서 정확히 수행되었다.
        - 너는 제공된 계산 결과를 "해석·선정·서술"만 한다.
        - 키워드를 새로 생성하거나,
          입력에 없는 값을 만들어내서는 안 된다.

        ────────────────────────────────────
        ■ 제공되는 계산 결과 (수정·재계산 금지)
        ────────────────────────────────────
        - counts: 각 키워드의 평균 관심도 (int)
        - changeRate: 각 키워드의 관심도 변화 지표 C (double)
        - avgInterest: 전체 평균 관심도 (double)

        위 값들은 신뢰 가능한 확정값이며,
        너는 이 값을 변경하거나 다시 계산하지 않는다.

        ────────────────────────────────────
        ■ TrendAnalyzeResponse 출력 규칙
        ────────────────────────────────────
        - JSON ONLY 출력
        - '{' 로 시작하고 '}' 로 끝나야 한다
        - null 금지
        - 필드명, 구조, 순서 변경 금지
        - 추가 필드 생성 금지
        - 한국어로 출력

        ────────────────────────────────────
        ■ 주요 키워드(majorKeyword) 선정 기준
        ────────────────────────────────────
        - majorKeyword는 반드시 trendJson.keywords 배열에 포함된 키워드 중에서만 선택한다.
        - 새로운 키워드를 생성하거나 배열 외 값을 선택해서는 안 된다.
        - 아래 기준을 종합적으로 고려하여 "하나의 키워드만" 선정한다.

        [선정 기준]
        1) counts가 상대적으로 높은 키워드
           - 현재 시장에서 기본적인 관심도가 높은 기술

        2) changeRate(C 값)가 높은 키워드
           - 최근 7일 기준 관심도가 빠르게 상승 중인 기술

        3) metaNews 맥락
           - 뉴스에서 긍정/부정 이슈의 중심에 있는 기술
           - 채용 확대, 투자, 기술 확산 등의 흐름과 연결되는 키워드

        4) Concept Keyword 우선 규칙
           - Concept Keyword란,
             trendJson.keywords 중에서
             산업·기술의 상위 개념을 대표하는 키워드를 의미한다.
           - 외부 지식이 아니라,
             keywords 배열 내부의 의미 관계만을 기준으로 판단한다.
           - 가능하다면 Concept Keyword 중에서
             changeRate가 높은 키워드를 우선 선정한다.

        위 기준을 종합하여
        "현재 채용 시장에서 가장 주목할 기술 하나"만 선택하라.

        ────────────────────────────────────
        ■ wordCloud 선정 기준
        ────────────────────────────────────
        - wordCloud는 trendJson.keywords에 포함된
          "선정된 트렌드 키워드"와 의미적으로 연관된 키워드로만 구성한다.
        - 절대로 trendJson.keywords에 포함된 키워드 자체를 포함하지 않는다.
        - 단순 변형, 동의어, 축약어, 복수형은 모두 금지한다.

        [연관 키워드 정의]
        - 기술의 활용 분야
        - 서비스/플랫폼
        - 산업 적용 영역
        - 기술 스택 또는 생태계 구성 요소

        [제약 조건]
        - 명사 또는 검색어 형태
        - 2~10글자
        - 추상어 금지 (성장, 혁신, 변화 등)
        - 감정/평가 표현 금지
        - 8~12개 항목으로 구성

        [score 규칙]
        - score는 사용자의 직무(jobGroup, targetRole)와의 연관성을 나타낸다.
        - 범위는 30~100
        - 숫자는 상대적 강도를 표현하면 된다 (정확 계산 불필요)

        ────────────────────────────────────
        ■ industrySentiment 선정 기준
        ────────────────────────────────────
        - industrySentiment는 반드시 6개의 산업군으로 구성한다.
        - 산업군 이름은 고정 리스트를 사용하지 않는다.
        - 아래 정보를 종합하여 의미 있는 산업군을 생성한다:

        [판단 요소]
        1) jobGroup (사용자 직군)
        2) targetRole (사용자 직무)
        3) trendJson.keywords (기술/도메인 키워드)
        4) metaNews (뉴스 기반 시장 분위기)

        [산업군 생성 원칙]
        - 서로 겹치지 않는 산업군이어야 한다.
        - 사용자의 직무와 실제 채용 시장 흐름을 반영해야 한다.

        [감정 비율 규칙]
        - positive + neutral + negative = 100
        - metaNews의 이슈 흐름을 반드시 반영한다.
          - 긍정 뉴스 증가 → positive 비율 상승
          - 규제, 사고, 채용 축소 → negative 비율 상승

        ────────────────────────────────────
        ■ marketInsight 작성 기준
        ────────────────────────────────────
        - counts 평균, majorKeyword, industrySentiment 흐름을 종합하여 작성한다.
        - 단순 요약이 아니라 "시장 해석"이 되도록 작성한다.
        - 최소 3문장 이상 작성한다.

        ────────────────────────────────────
        ■ finalSummary 작성 기준
        ────────────────────────────────────
        - industrySentiment에 포함된 6개 산업군 각각에 대해
          감정 비율이 그렇게 결정된 이유를 설명한다.
        - metaNews에 기반한 실제 사건·이슈를 근거로 사용한다.
        - 마지막 문단에서 전체 기술·채용 시장 분위기를 종합 요약한다.
        ────────────────────────────────────
        """;

    String userPrompt = """
        아래는 TrendDataContext(JSON)와
        백엔드에서 계산된 확정 지표들이다.

        이 데이터들을 기반으로
        TrendAnalyzeResponse를 생성하라.

        ────────────────────
        [TrendDataContext]
        ────────────────────
        %s

        ────────────────────
        [ComputedMetrics]
        ────────────────────
        counts: %s
        changeRate: %s
        avgInterest: %s
        """.formatted(
        mapper.writeValueAsString(context),
        counts.toString(),
        changeRateMap.toString(),
        avgInterest);

    String llmResult = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();

    TrendAnalyzeResponse response = mapper.readValue(llmResult, TrendAnalyzeResponse.class);
    log.info("[FINAL SUMMARY] {}", response.getInsightJson().getFinalSummary());

    return response;
  }

  /*
   * 계산 메서드들
   */

  // ratio를 찾는 메서드
  // { "data": [ { "ratio": 10 } ] }
  // { "results": [ { "data": [ { "ratio": 10 } ] } ] } 이렇게 오는 경우가 다를수도 있다고 함
  private List<Integer> extractRatios(Object raw) {
    // instanceof로 Map,List 구조 확인(빌드 에러 발생할 수 있다고 해서 사용함)
    if (!(raw instanceof Map<?, ?> map))
      return List.of();

    Object data = map.get("data");

    if (data instanceof List<?> list) {
      return extractRatioList(list);
    }

    Object results = map.get("results");

    if (results instanceof List<?> rList && !rList.isEmpty()) {
      Object first = rList.get(0);
      if (first instanceof Map<?, ?> fMap) {
        Object d = fMap.get("data");
        if (d instanceof List<?> list) {
          return extractRatioList(list);
        }
      }
    }

    return List.of();
  }

  // ratio 추출하는 메서드
  private List<Integer> extractRatioList(List<?> list) {
    List<Integer> ratios = new ArrayList<>();
    for (Object o : list) {
      // Map 구조인지 확인
      if (o instanceof Map<?, ?> m) {
        // Map 안에서 ratio 값 추출
        Object r = m.get("ratio");
        // ratio가 숫자 타입이면 int 값으로 변환하여 리스트 추가
        if (r instanceof Number n) {
          ratios.add(n.intValue());
        }
      }
    }
    return ratios;
  }

  // 각각의 키워드의 평균 관심도를 구하는 메서드
  private int calculateAverage(List<Integer> ratios) {
    if (ratios == null || ratios.isEmpty())
      return 0;
    // ratio 평균을 구하고 반올림하여 정수로 반환
    return (int) Math.round(
        ratios.stream().mapToInt(Integer::intValue).average().orElse(0));
  }

  // 주요 키워드 선정을 위한 변화율 메서드
  private double calculateChangeRate(List<Integer> ratios) {
    if (ratios.size() < 7)
      return 0.0;

    // 초반 3일 평균 관심도 계산
    double x = (ratios.get(0) + ratios.get(1) + ratios.get(2)) / 3.0;

    // 후반 3일 평균 관심도 계산
    double y = (ratios.get(4) + ratios.get(5) + ratios.get(6)) / 3.0;

    // 분자가 0을 방지하기 위해 +1
    return y / (x + 1);
  }

  // 전체 키워드의 평균 관심도
  private double calculateAvgInterest(List<Integer> counts) {
    if (counts == null || counts.isEmpty())
      return 0.0;

    // 키워드 평균 관심도 계산
    return Math.round(
        counts.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;
  }
}
