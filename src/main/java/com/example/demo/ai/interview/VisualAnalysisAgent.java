package com.example.demo.ai.interview;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

// 영상(이미지 프레임) 분석 에이전트

@Component
@Slf4j
public class VisualAnalysisAgent {

    /*==============
      필드
    =============== */

    // ChatClient
    private ChatClient chatClient;
    
    
    /*==============
      생성자
    =============== */
    public VisualAnalysisAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    

    /*==============
      메소드
    =============== */

    // 1) 이미지 한 장 분석=================================================================================
    private String analyzeOneFrame(String contentType, byte[] bytes) {

        // 1. 이미지 데이터를 ByteArrayResource로 변환
        Resource imageResource = new ByteArrayResource(bytes);

        // 2. 미디어 객체 생성
        MimeType mimeType = (contentType != null) 
            ? MimeType.valueOf(contentType)
            : MimeType.valueOf("image/png");

        Media media = Media.builder()
            .mimeType(mimeType)
            .data(imageResource)
            .build();

        // 3. 시스템 메시지
        SystemMessage systemMessage = SystemMessage.builder()
            .text("""
               당신은 면접 영상 분석 전문가입니다.
               제공되는 이미지(면접 프레임)를 분석하여
               - 표정
               - 자세
               등의 비언어적 요소에 대해 피드백을 작성하세요.
            """)
            .build();

        // 4. 사용자 메시지
        UserMessage userMessage = UserMessage.builder()
            .text("아래 이미지를 분석해주세요")
            .media(media)
            .build();

        // 5. LLM에 메시지 전달 + 응답 스트림 반환
        String response = chatClient.prompt()
            .messages(systemMessage, userMessage)
            .call()
            .content();

        return response;
    }
    

    // 2) MultipartFile 리스트 → 분석 결과 리스트===================================================================================
    public List<String> analyzeFrames(List<MultipartFile> frames) throws Exception {
        if (frames == null || frames.isEmpty()) {
            return List.of();
        }

        List<String> results = new ArrayList<>();

        for (MultipartFile frame : frames) {

            // 이미지의 바이트 배열 얻기
            byte[] bytes = frame.getBytes();
            
            // 이미지의 MIME 타입 얻기
            String contentType = frame.getContentType();

            // 이미지 한 장 분석기에 바이트 배열 + MIME 타입 넘기기
            String analysis = analyzeOneFrame(contentType, bytes);

            // 분석 결과 리스트에 저장
            results.add(analysis);
        }

        log.info("=== Visual feedback result count: {} ===", results.size());

        return results;
    }


}
