package com.example.demo.ai.interview;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.interview.dto.request.QuestionRequest;
import com.example.demo.interview.dto.response.AiQuestionResponse;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;

// 면접 질문 생성 에이전트
@Component
public class CreateQuestionAgent {

  @Autowired
  private PdfReader pdfReader; // PDF 파일의 텍스트 추출

  @Autowired
  private MemberDao memberDao;

  // ChatClient
  private ChatClient chatClient;

  public CreateQuestionAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public List<AiQuestionResponse> createQuestion(QuestionRequest request) throws Exception {
    
    // 사용자의 직무, 직군 추출
    Member member = memberDao.findById(request.getMemberId());
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    String keywords = (request.getKeywords() == null || request.getKeywords().isEmpty())
        ? ""
        : String.join(", ", request.getKeywords());

    // PDF에서 텍스트 추출
    String documentText = "";
    if (request.getDocumentFileData() != null) {
      documentText = pdfReader.read(request.getDocumentFileData());
    }

    // LLM이 JSON 배열을 반환
    // JSON 변환 (JSON 배열을 List로 바로 변환 불가능 -> 배열 변환 -> 리스트 변환)
    BeanOutputConverter<AiQuestionResponse[]> converter = new BeanOutputConverter<>(AiQuestionResponse[].class); 
    String format = converter.getFormat();

    // System prompt
    String system = """
        당신은 실제 기업의 면접관입니다.
        면접 유형에 따라 종합 면접과 직무 면접 질문을 생성하세요.
        종합 면접일 경우 인성 면접 위주로 생성하세요.

        반드시 다음 규칙을 지키세요:

        1) 최종 출력은 JSON 배열이어야 한다.
        2) JSON 배열의 길이는 반드시 정확히 5개여야 한다.
        3) 각 항목은 { "aiQuestion": "내용" } 형식이어야 한다.
        4) JSON 외의 텍스트는 절대 출력하지 않는다.
        5) 입력 정보가 부족하더라도 반드시 일반적인 인성 또는 직무 기반 질문을 생성하여 5개를 채워야 한다.

        출력 형식:
        %s
        """.formatted(format);

    // User prompt
    String prompt = """
        다음 정보를 기반으로 5개의 면접 질문을 생성하세요.

        [사용자의 직무] %s
        [사용자의 직군] %s
        [면접 유형] %s
        [지원 기업명] %s
        [선택된 키워드] %s
        [서류 내용] %s
        면접 유형이 "종합"이면 인성/가치관/경험 중심 질문만 생성합니다.
        면접 유형이 "직무"이면 기술 기반 질문만 생성합니다.

        ※ 만약 일부 정보가 비어 있어도, 반드시 5개의 질문을 생성하세요.
        """
        .formatted(jobGroup, jobRole, request.getType(), request.getTargetCompany(), keywords, documentText);

    // LLM 호출
    String responseJson = chatClient.prompt()
        .system(system)
        .user(prompt)
        .call()
        .content();


    // JSON -> DTO 배열
    AiQuestionResponse[] arr = converter.convert(responseJson);

    // 배열 -> List
    List<AiQuestionResponse> list = Arrays.asList(arr);

    return list;
  }
}
