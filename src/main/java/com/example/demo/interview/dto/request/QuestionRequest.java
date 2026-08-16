package com.example.demo.interview.dto.request;

import java.util.List;

import lombok.Data;

// AI 질문 생성 요청
@Data
public class QuestionRequest {
  private Integer memberId; //희망 직무/직군 가져와야 해서 사용자 ID
  private String type;  //면접 유형(종합/직무)
  private String targetCompany; //희망 기업
  private List<String> keywords;  //선택된 키워드들
  
  private String documentFileName;  //서류 파일 이름
  private String documentFileType;  //서류 파일 타입
  private byte[] documentFileData;  //서류 바이너리 데이터
}
