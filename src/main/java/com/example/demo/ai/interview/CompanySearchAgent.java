package com.example.demo.ai.interview;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 기업명 자동 추출 Agent
 * - 사용자가 입력한 검색어를 기반으로 네이버 검색 API 호출
 * - 검색 결과의 제목(title) 텍스트를 수집 및 HTML 태그 제거
*/

@Component
public class CompanySearchAgent {

  @Value("${naver.api.client-id}")
  private String clientId;

  @Value("${naver.api.client-secret}")
  private String clientSecret;

  // ChatClient
  private ChatClient chatClient;
  // 네이버 API 호출
  private WebClient webClient;

  public CompanySearchAgent(ChatClient.Builder chatClientBuilder,
                            WebClient.Builder webClientBuilder
  ) {
    this.chatClient = chatClientBuilder.build();
    this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com")
                .build();
  }
  
  public List<String> searchCompanyNames(String query) {

    if (query == null || query.isBlank()) { // 아무것도 입력 안했을 경우 빈 리스트 리턴
        return new ArrayList<>();
    }

    String response = webClient.get()
      .uri(uriBuilder -> uriBuilder
        .path("/v1/search/webkr.json") // 웹문서 검색 API
        .queryParam("query", query)
        .queryParam("display", 10) // 검색 결과 개수
        .queryParam("sort", "date") // 날짜순
        .build()
      )
      .header("X-Naver-Client-Id", clientId)
      .header("X-Naver-Client-Secret", clientSecret)
      .retrieve() // 응답 준비
      .bodyToMono(String.class) // Mono<String> 리턴
      .block(); // 실제 String으로 변환

    if (response == null) { // 응답이 null이면 빈 리스트 리턴
      return new ArrayList<>();
    }

    JSONArray items = new JSONObject(response).getJSONArray("items"); // 문자열을 JSON 객체로 변환 후 itmes 배열 반환
    List<String> titles = new ArrayList<>(); // 기업명 후보

    for (int i = 0; i < items.length(); i++) {
      String title = items.getJSONObject(i).getString("title"); // 각 검색 결과의 title 가져오기
      titles.add(title.replaceAll("<[^>]*>", "")); // HTML 제거 후 기업명 후보에 추가
    }

    // 기업명 후보중에서 실제 기업만 추출하기 위한 System Prompt
    String system = """
        당신은 JSON만 출력하는 시스템입니다.

        아래 리스트를 기반으로 '기업명처럼 보이는 단어'만 추출하여
        반드시 JSON 배열 형식 ONLY로 출력하세요.

        절대 문장, 설명, 객체, 키:값 구조를 포함하지 마세요.
        절대 따옴표가 빠진 문자열을 넣지 마세요.

        반드시 이런 형식이어야 함:
        ["네이버", "카카오", "삼성전자"]
        """;

    // ["삼성전자",
    // "카카오",
    // "네이버"]
  
    String prompt = String.join("\n", titles); // 원소 사이를 \n으로 연결

    // 삼성전자
    // 카카오
    // 네이버

    String aiResult = chatClient.prompt()
      .system(system)
      .user(prompt)
      .call()
      .content();

      
    // LLM 결과를 JSONArray로 변환
    JSONArray arr = new JSONArray(aiResult);

    List<String> result = new ArrayList<>();
    // 리스트로 변환
    for (int i = 0; i < arr.length(); i++) {
      result.add(arr.getString(i));
    }

    return result;
  }
}
