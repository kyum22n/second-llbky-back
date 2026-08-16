package com.example.demo.ai.newstrend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.response.TrendKeywordResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// 뉴스 키워드 필터링해서 트렌드로 넘겨주는 에이전트

@Component
public class TrendKeywordExtractionAgent {

    @Autowired
    private NewsSummaryDao newsSummaryDao;
    @Autowired
    private MemberDao memberDao;

    @Autowired
    private ObjectMapper mapper;

    private ChatClient chatClient;

    public TrendKeywordExtractionAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public TrendKeywordResponse execute(int memberId) throws Exception {

        Member member = memberDao.findById(memberId);

        // 1) 뉴스 50개 조회
        List<NewsSummary> newsList = newsSummaryDao.selectLatestNewsByMemberId(memberId, 50);

        // 2) 키워드 후보 풀 생성
        List<String> keywordPool = new ArrayList<>();

        for (NewsSummary n : newsList) {
            if (n.getKeywordsJson() == null)
                continue;

            // JSON 파싱
            List<Map<String, String>> list = mapper.readValue(n.getKeywordsJson(),
                    new TypeReference<List<Map<String, String>>>() {
                    });

            for (Map<String, String> k : list) {
                keywordPool.add(k.get("keyword"));
            }
        }

        Map<String, Integer> freq = new HashMap<>();

        for (String k : keywordPool) {
            freq.put(k, freq.getOrDefault(k, 0) + 1);
        }

        StringBuilder keywordStats = new StringBuilder();

        // 키워드 빈도 리스트 문자열 (ex -AI: 11회) LLM에게 계산 줄이게 하기 위해
        freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> {
                    keywordStats.append("- ")
                            .append(e.getKey())
                            .append(": ")
                            .append(e.getValue())
                            .append("회\n");
                });

        // Converter
        BeanOutputConverter<TrendKeywordResponse> converter = new BeanOutputConverter<>(TrendKeywordResponse.class);

        String format = converter.getFormat();

        String systemPrompt = """
                        너는 뉴스에서 추출된 키워드를
                        '트렌드 검색에 적합한 키워드 후보'로 정제하는 필터링 에이전트다.

                        너는 키워드를 새로 생성하지 않는다.
                        너는 주어진 키워드 중에서만 선택한다.
                        모든 판단은 jobGroup, targetRole 기준으로 수행한다
                        해당 직무/직군과 직접적인 연관이 없는 키워드는 제외한다


                        목표:
                        - 네이버 검색 트렌드 API에 전달할 키워드 후보를 선정한다.

                        - 반드시 뉴스 keywordPool에서만 선택할 것
                        - 단일 명사 또는 검색어 형태일 것
                        - 2~10글자
                        - 반드시 10개의 키워드를 선정할 것
                        - 일반명사 / 기업명 / 기술명 허용
                        - 추상어(성장, 혁신, 변화, 전략 등) 금지
                        - 감정/평가 표현 금지
                        - 직무/채용 맥락에서 검색 의미가 있을 것

                        출력은 반드시 다음 JSON 형식을 따른다:
                        %s
                    """.formatted(format);

        String userPrompt = """
                    [직군 정보]
                    jobGroup: %s
                    targetRole: %s

                    [뉴스 기반 키워드 후보 목록]
                    %s

                    [뉴스 기반 키워드 빈도]
                    %s

                    위 뉴스 기반 키워드 후보 목록과 빈도를 참고하여,
                    트렌드 검색에 적합한 키워드 10개를 선정해라
                """.formatted(
                member.getJobGroup(),
                member.getJobRole(),
                keywordPool.toString(),
                keywordStats.toString());

        String json = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        TrendKeywordResponse response = converter.convert(json);

        return response;
    }

}
