package com.example.demo.ai.interview;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

// PDF 파일의 텍스트 읽기
@Component
public class PdfReader {

  public String read(byte[] fileData) throws Exception {
    // PDF 문서를 메모리(byte[])에서 바로 로드
    PDDocument document = PDDocument.load(fileData);

     // PDF 내부 텍스트를 추출하는 유틸리티 클래스
    PDFTextStripper stripper = new PDFTextStripper();
    // 전체 PDF 페이지의 텍스트 추출
    String text = stripper.getText(document);

    // 메모리 누수 방지
    document.close();
    return text;
  }
}
