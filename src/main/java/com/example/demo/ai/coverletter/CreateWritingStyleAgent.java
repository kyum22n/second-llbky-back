package com.example.demo.ai.coverletter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.dto.response.WritingStyle;
import com.example.demo.coverletter.entity.CoverLetter;

// 자소서 문체 버전 생성하는 에이전트

@Component
public class CreateWritingStyleAgent {
    
    // DAO
    @Autowired
    private CoverLetterDao coverLetterDao;

    // ChatClient
    private ChatClient chatClient;
    
    // ChatClientBuilder 주입
    public CreateWritingStyleAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 문체 3가지 버전 생성
    public WritingStyle execute(int coverletterId, String section) {

        // 1. DB에서 자소서 불러오기
        CoverLetter coverLetter = coverLetterDao.selectOneCoverLetter(coverletterId);
        if (coverLetter == null) {
            throw new RuntimeException("CoverLetter not found");
        }

        // 2. 요청된 section만 추출
        String text = null;
        // 선택된 section의 내용을 text에 대입
        switch (section) {
            case "supportMotive":
                text = coverLetter.getSupportMotive();
                break;
            case "growthExperience":
                text = coverLetter.getGrowthExperience();
                break;
            case "jobCapability":
                text = coverLetter.getJobCapability();
                break;
            case "futurePlan":
                text = coverLetter.getFuturePlan();
                break;
            default:
                throw new IllegalArgumentException("Invalid section");
        }

        // 3. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<WritingStyle> converter = new BeanOutputConverter<>(WritingStyle.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 4. 프롬프트 구성
        String prompt = """
            아래 텍스트(자기소개서 항목)를 기반으로 3가지 문체 버전을 생성하세요.
            반드시 아래 JSON 형식을 그대로 채워서 출력하세요:
            %s

            --- 문체 버전 구조 ---
            {
                "simpleVersion": "간결한 버전",
                "caseVersion": "사례 중심 버전",
                "visionVersion": "비전 제시형 버전"
            }

            --- 원본 텍스트 ---
            %s
        """.formatted(format, text);

        // 5. LLM 호출
        String json = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        // 6. JSON -> DTO 변환
        WritingStyle writingStyle = converter.convert(json);

        return writingStyle;
    }
}
