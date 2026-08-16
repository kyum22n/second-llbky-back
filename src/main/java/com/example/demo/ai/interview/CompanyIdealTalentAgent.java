package com.example.demo.ai.interview;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.interview.dto.response.CompanySearchResponse;

/**
 * 기업의 인재상과 핵심 가치 요약 Agent
 */

@Component
public class CompanyIdealTalentAgent {

  // ChatClient
  private ChatClient chatClient;

  public CompanyIdealTalentAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public CompanySearchResponse searchCompanyIdealTalent(String companyName) {

    // JSON 변환
    BeanOutputConverter<CompanySearchResponse> converter = new BeanOutputConverter<>(CompanySearchResponse.class);
    String format = converter.getFormat();

    String system = """
        당신은 채용 전문 분석 AI입니다.
        사용자가 입력한 기업의 인재상과 핵심가치를 인터넷 기반 지식을 참고해 요약하세요.

        반드시 JSON만 출력하세요.
        형식: %s
        """.formatted(format);

    String responseJson = chatClient.prompt()
        .system(system)
        .user(companyName + "기업의 인재상과 핵심가치를 요약하세요")
        .call()
        .content();

    // JSON -> DTO 변환
    CompanySearchResponse response = converter.convert(responseJson);

    return response;
  }
}
