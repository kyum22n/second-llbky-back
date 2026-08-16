package com.example.demo.ai.interview;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.demo.interview.dao.InterviewAnswerDao;

import lombok.extern.slf4j.Slf4j;

// 음성 분석 에이전트

@Component
@Slf4j
public class STTAgent {

    /*==============
      필드
    =============== */
    
    // STT
    private OpenAiAudioTranscriptionModel openaiAudioTranscriptionModel;
    
    // DAO
    @Autowired
    private InterviewAnswerDao interviewAnswerDao;
    
    
    /*==============
      생성자
    =============== */
    public STTAgent(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel) {
        this.openaiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    }
    

    /*==============
      메소드
    =============== */

    // 1) STT 변환================================================================================================
    private String stt(String fileName, byte[] bytes) {
        log.info("=== [STT CALL] ===");
        log.info("fileName: {}", fileName);
        log.info("bytes length: {}", (bytes != null ? bytes.length : -1));

        // 1. Resource 객체 생성
        Resource audioResource = new ByteArrayResource(bytes) {
            // 파일 이름
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        // 2. AudioTranscriptionModel을 생성해서 어떻게 변환할지 옵션 지정, 힌트 제공
        AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
            .model("whisper-1")
            .language("ko")
            .build();

        // 3. 프롬프트 구성
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

        // 4. LLM에 프롬프트 전달 + 응답
        AudioTranscriptionResponse response = openaiAudioTranscriptionModel.call(prompt);
        String text = response.getResult().getOutput();

        log.info("=== [STT RESULT] === {}", text);
        
        return text;
    }
    
    // 2) 변환된 텍스트를 DB에 저장====================================================================
    public String sttSave(int answerId, String fileName, byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            log.warn("[STT] empty bytes, skip STT");
            return "";
        }

        // STT 변환 호출
        String text = stt(fileName, bytes);

        // 변환된 텍스트 저장
        interviewAnswerDao.updateAnswerText(answerId, text);

        return text;
    }
}
