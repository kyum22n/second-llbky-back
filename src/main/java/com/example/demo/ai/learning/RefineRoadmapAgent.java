package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.request.RoadmapRefineRequest;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;

@Component
public class RefineRoadmapAgent {
  private ChatClient chatClient;

  public RefineRoadmapAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public AiCreateRoadmapResponse refineRoadmap(RoadmapRefineRequest request) {

    BeanOutputConverter<AiCreateRoadmapResponse> converter = new BeanOutputConverter<>(AiCreateRoadmapResponse.class);
    String format = converter.getFormat();

    String system = """
            당신은 이미 생성된 학습 로드맵을 “구조와 내용을 유지한 채” 세부 내용을 보강하는 역할을 합니다.

            아래 제공되는 기존 로드맵(JSON)은 사용자의 직무/목표/분야에 맞게 이미 설계된 구조입니다.

            🔒 절대 변경 불가 규칙 (반드시 지켜야 함):

            1. Week(주차) 수를 변경할 수 없다.  
              - 기존이 4주면 4주 그대로 유지해야 한다.
            2. Day(일차) 수를 변경할 수 없다.
              - 기존이 7일차면 7일차 그대로 유지해야 한다.
            3. 각 Week의 제목(title)을 변경할 수 없다.
            4. 각 Week 내부의 days(dayNumber)의 개수를 변경할 수 없다.  
              - 새로운 Day를 추가하거나 삭제하거나 숫자를 줄이면 안 된다.
            5. dayNumber의 순서를 바꾸면 안 된다.
            6. 기존 JSON의 구조는 어떤 경우에도 재설계해서는 안 된다.
            7. “이론 중심으로 해주세요”와 같은 요청일지라도  
              → 구조 변경이 아니라  
              → 기존 day의 텍스트를 이론 설명 중심으로 보강하라는 의미로 해석해야 한다.
            8. 기존 JSON 속 텍스트는 최대한 유지하며, 그 안을 보완하는 방식으로만 수정한다.
            9. 새로운 Week나 새로운 커리큘럼(기초, 심화 등)을 생성하면 안 된다.

            출력은 아래 JSON 포맷과 완전히 동일한 구조로 생성한다:
            %s
        """.formatted(format);

    String prompt = """
            [로드맵 생성 정보]
            - 학습 목적: %s
            - 관심 기술: %s
            - 하루 학습 가능 시간: %d

            [기존 로드맵 JSON]
            %s

            [사용자 수정 요청]
            %s

            수정 요청은 기존 로드맵의 “텍스트 보완” 의미이지 기존 week/day 구조나 제목을 변경하라는 의미가 아닙니다.
        """.formatted(request.getPurposes(), request.getSkills(), request.getStudyHours(), request.getOriginalRoadmapJson(), request.getUserFeedback());

    // LLM 호출
    String json = chatClient.prompt()
        .system(system)
        .user(prompt)
        .call()
        .content();

    // JSON → DTO 변환
    AiCreateRoadmapResponse result = converter.convert(json);

    return result;
  }

}
