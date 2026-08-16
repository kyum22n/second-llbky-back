package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.request.RoadmapRequest;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;

@Component
public class CreateRoadmapAgent {

  private ChatClient chatClient;

  public CreateRoadmapAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public AiCreateRoadmapResponse generateRoadmap(RoadmapRequest request) {

    // Bean 객체 -> JSON 출력 변환기 생성
    BeanOutputConverter<AiCreateRoadmapResponse> converter = new BeanOutputConverter<>(AiCreateRoadmapResponse.class);

    // DTO 구조 제공 -> JSON 출력 포맷 지정
    String format = converter.getFormat();

    // 시스템 프롬프트 구성
    String system = """
        당신은 모든 분야의 교육을 설계할 수 있는 전문 학습 코치입니다.
        개발자뿐 아니라, 기획, 디자인, 마케팅, 경영, 자격증, 공무원, 외국어, 취미 등 어떤 분야라도 고품질의 맞춤형 학습 로드맵을 설계해야 합니다.
        학습 플랜은 4주차 기준이며 각 주차는 무조건 7일이어야 합니다. 각 일차별로 계획을 작성하세요.
        절대로 코드블록(```), 백틱(``) 등을 포함하지 마세요.

        [학습 로드맵 생성 규칙]
        1. 사용자가 입력한 직무·분야·목적·기술을 기반으로 맞춤형 로드맵을 구성합니다.
        2. 로드맵은 4주차, 각 주차 7일로 구성합니다.
        3. 주차에는 title, goal, learningWeekSummary를 포함해야 합니다.
        4. 각 일차(day)의 content는 아래 기준을 반드시 충족해야 합니다.

        [content 상세 작성 규칙 — 매우 중요]
        각 Day의 content는 다음 7가지 요소를 **반드시 포함**해야 합니다.
        짧게 요약하는 것이 아니라, 실제 교재 수준으로 구체적이어야 합니다.

        A. 주제에 대한 핵심 개념 설명 (해당 직무/분야에 최적화된 내용)
        B. 반드시 세부 항목을 나눠서 구조적으로 서술 (예: 1. 개념, 2. 원리, 3. 적용, 4. 예시)
        C. 사용자가 입력한 목적과 직접 연결되는 학습 포인트
           - 취업 준비 → 실무 적용 / 포트폴리오 준비 / 실무 기술
           - 자격증 취득 → 자격증 개념 / 출제 포인트 / 문제 유형
           - 커리어 전환 → 기초 다지기 / 개념 정복 / 실습 위주
           - 자기계발 → 개념 설명 + 실전 활용
        D. 난이도와 단계별 학습 방법
        E. 초보자가 자주 실수하는 부분 / 오해하는 개념 / 주의사항
        F. 실제 적용 사례 또는 실무 예시
        G. 연습 문제 / 실습 아이디어 / 퀴즈 등의 구체적 활동

        ❗ content 예시
        1) 아래 스키마를 직접 입력하여 'employees' 테이블을 생성한다.
        sql 
        CREATE TABLE employees (emp_id INT PRIMARY KEY, emp_name VARCHAR(50) NOT NULL,  department VARCHAR(30),  salary INT);
        2) 테이블 생성 후 DESC employees 또는 SELECT column_name, data_type FROM information_schema.columns WHERE table_name='employees'; 로 컬럼 구조를 확인한다.
        3) 체크포인트:
        - PRIMARY KEY 선언 여부 확인
        - NOT NULL 제약 정확히 입력했는지 확인
        - 컬럼명 오기 없는지 재확인
        4) 결과물:
        - 정상적으로 생성된 employees 테이블

        형식(JSON만 출력):
        %s
        """.formatted(format);

    // 사용자 프롬프트 구성
    String prompt = """
        사용자 학습 정보
        - 하루 공부 가능 시간: %d
        - 학습 목적: %s
        - 관심 기술: %s

        위 정보를 기반으로 4주차 맞춤 학습 로드맵을 설계하세요.
        """.formatted(request.getStudyHours(), request.getPurposes(), request.getSkills());

    // LLM 호출
    String responseJson = chatClient.prompt()
        .system(system)
        .user(prompt)
        .call()
        .content();

    // JSON -> DTO 변환
    AiCreateRoadmapResponse response = converter.convert(responseJson);

    return response;
  }
}
