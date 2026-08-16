package com.example.demo.ai.newstrend;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.response.SentimentResponse;
import com.example.demo.newstrend.entity.NewsSummary;

// 이슈에 대한 여론을 기반으로 감정을 분석

@Component
public class SentimetalAnalysisAgent {

    @Autowired
    private NewsSummaryDao newsSummaryDao;

    private ChatClient chatClient;

    public SentimetalAnalysisAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public SentimentResponse excute(int memberId, int limit) {

        // 1) DB에서 최신 뉴스 50개 조회
        List<NewsSummary> newsList = newsSummaryDao.selectLatestNewsByMemberId(memberId, limit);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < newsList.size(); i++) {
            NewsSummary n = newsList.get(i);

            sb.append("[").append(i + 1).append("]\n")
            .append("제목: ").append(n.getTitle()).append("\n")
            .append("요약: ").append(n.getSummaryText()).append("\n")
            .append("상세요약: ").append(n.getDetailSummary()).append("\n\n");
        }

        String newsText = sb.toString();

        BeanOutputConverter<SentimentResponse> converter = new BeanOutputConverter<>(SentimentResponse.class);

        String format = converter.getFormat();

        String sysprompt = """
                뉴스 50개를 분석해 다음 구조로 결과를 출력하라:

                1) 주요 이슈별로 기사를 클러스터링한다.
                2) 각 이슈가 시장 분위기에 미친 감정(긍정/중립/부정)을 판단한다.
                3) 각 이슈의 기사 수, 해당 감정이 전체 감정 신호에 기여한 비율(impact)을 계산한다.
                4) 산업 전체의 감정 신호를 긍정/중립/부정으로 분류한 뒤,
                   세 감정 비율의 총합이 100이 되도록 정규화하여 sentimentRatio를 생성한다.
                5) 최종적으로, 산업의 전체 분위기와 왜 그런 결과가 나왔는지를 ‘구체적 사건 기반’으로 요약하라.

                결과는 아래 JSON 스키마로 출력하라:

                {
                  "industry": "",
                  "sentimentRatio": {
                    "positive": number,
                    "neutral": number,
                    "negative": number
                  },
                  "issues": [
                    {
                      "issue": "",
                      "sentiment": "",
                      "articleCount": number,
                      "impact": "",
                      "reason": ""
                    }
                  ],
                  "overallSummary": ""
                }

                        """.formatted(format);

        String userPrompt = """
            아래는 관심 직무 관련 요약된 최신 뉴스 목록이다.
            title, summaryText, detailSummary 를 기반으로 분석하라.

            --- 뉴스 목록 시작 ---
            %s
            --- 뉴스 목록 끝 ---
            """.formatted(newsText);

        String json = chatClient.prompt()
                .system(sysprompt)
                .user(userPrompt)
                .call()
                .content();

        SentimentResponse response = converter.convert(json);

        return response;
    }
}
