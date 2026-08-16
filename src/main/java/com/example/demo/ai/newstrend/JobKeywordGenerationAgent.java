package com.example.demo.ai.newstrend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 직군 맞춤 키워드 생성 Agent
 * - 사용자의 직군과 직무 정보를 기반으로 뉴스 검색용 키워드를 AI로 생성
 * - 기존 검증된 키워드들을 참고하여 더 정확한 키워드 생성
 */
@Component  // Spring Bean으로 등록하여 자동 주입 가능하게 설정
@Slf4j      // Lombok 로깅 기능 활성화 (log.info, log.error 등 사용 가능)
public class JobKeywordGenerationAgent {

    private final ChatClient chatClient;  // Spring AI의 ChatClient (AI 모델과 통신)

    // ✅ 기존 검증된 직군별 키워드 매핑 (AI 참고용)
       private static final Map<String, List<String>> REFERENCE_JOB_KEYWORDS = Map.of(
        "개발", Arrays.asList(  // Arrays.asList()로 List 생성
            "개발자 채용", "백엔드 채용", "프론트엔드 채용", "풀스택 개발자",
            "소프트웨어 엔지니어", "프로그래머", "코딩", "Java", "Python", "React", "Spring",
            "IT", "기술", "소프트웨어", "엔지니어링", "개발", "프로그래밍", "개발자"),

        "AI/데이터", Arrays.asList(
            "데이터 사이언티스트", "데이터 엔지니어", "AI 개발자", "머신러닝 엔지니어",
            "빅데이터", "데이터 분석가", "인공지능", "딥러닝", "ML", "IT", "기술", "데이터", "AI", "분석"),

        "디자인", Arrays.asList(
            "UI 디자이너", "UX 디자이너", "웹디자인", "그래픽 디자이너", "프로덕트 디자이너",
            "디자인 채용", "포토샵", "피그마", "일러스트", "브랜딩",
            "디자인", "디자이너", "크리에이티브"),

        "기획", Arrays.asList(
            "기획자 채용", "서비스 기획", "상품 기획", "사업 기획", "전략 기획",
            "기획 업무", "기획 직무", "비즈니스 분석"),

        "PM", Arrays.asList(
            "PM 채용", "프로젝트 매니저 채용", "프로덕트 매니저 채용",
            "PO 채용", "프로덕트 오너", "PMO", "애자일 코치",
            "스크럼 마스터", "프로젝트 관리", "애자일", "스크럼",
            "칸반", "백로그", "스프린트"),

        "마케팅", Arrays.asList(
            "마케팅 매니저", "디지털 마케팅", "퍼포먼스 마케팅", "콘텐츠 마케팅",
            "브랜드 마케팅", "마케팅 기획", "광고", "SNS 마케팅", "SEO",
            "마케팅", "마케터"),

        "영업", Arrays.asList(
            "영업 대표", "세일즈", "비즈니스 개발", "B2B 영업", "고객 관리",
            "영업 기획", "계정 관리", "Sales",
            "영업", "세일즈"),

        "경영", Arrays.asList(
            "경영", "경영관리", "경영기획", "전략", "경영지원", "임원", "관리자", "경영", "관리", "임원"),

        "교육", Arrays.asList(
            "교육 기획", "강사", "교육 콘텐츠", "이러닝", "교육 프로그램", "연수", "교육생", "교육", "강의", "트레이닝", "부트캠프", "양성", "과정"),

        "기타", Arrays.asList(
            "인사", "총무", "재무", "회계", "법무", "운영", "고객서비스", "품질관리", "지원", "관리")
    );

    // 생성자: Spring AI ChatClient 주입받아 초기화
    public JobKeywordGenerationAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();  // ChatClient 빌더로 인스턴스 생성
    }

    /**
     * 직군과 직무에 맞는 뉴스 검색 키워드 생성
    
     */
    public List<String> generateJobKeywords(String jobGroup, String jobRole) {
        
        // 직군 정보가 없으면 기본 키워드 반환 (null 또는 빈 문자열 체크)
        if (jobGroup == null || jobGroup.isBlank()) {
            log.warn("직군 정보가 없어 기본 키워드 반환");  // 경고 로그 출력
            return List.of("채용", "취업", "일자리");  // List.of()로 불변 리스트 반환
        }

        // ✅ 기존 검증된 키워드 참고
        // getOrDefault(): jobGroup이 Map에 없으면 빈 리스트 반환 (NullPointerException 방지)
        List<String> referenceKeywords = REFERENCE_JOB_KEYWORDS.getOrDefault(jobGroup, List.of());
        
        // 참고 키워드를 쉼표로 연결한 문자열 생성 (AI 프롬프트에 포함하기 위해)
        String referenceKeywordsStr = String.join(", ", referenceKeywords);

        // AI 시스템 프롬프트 (AI의 역할과 출력 형식 정의)
        // Text Block (""") 사용: 여러 줄 문자열을 깔끔하게 작성 (Java 15+)
        String system = """
            당신은 취업 준비생을 위한 뉴스 검색 키워드 생성 시스템입니다.

            목표:
            - 구직자가 채용 동향, 채용 공고, 채용 시장 변화를 파악하는 데
            직접적으로 도움이 되는 키워드만 생성합니다.
            - 직군/직무와 무관하거나 취업과 연결되지 않는 키워드는 생성하지 않습니다.

            핵심 판단 기준 (매 키워드마다 내부 검증):
            - 해당 키워드로 검색했을 때 "채용, 모집, 공고, 인재, 커리어"와
            직접 연결된 뉴스가 다수 노출될 것
            - 취업 준비생이 실제로 검색할 법한 표현일 것
            - 직무를 명확히 특정할 수 있을 것

            우선 생성해야 할 키워드 유형:
            - 채용 공고에서 실제로 사용되는 표현
            - 해당 직무의 채용 수요를 나타내는 키워드
            - 신입 / 경력 / 인턴 / 공채 등 고용 형태와 연결되는 키워드
            - 해당 직무 인력 채용과 직접 연결된 산업 이슈
            - 기술 트렌드=

            출력 조건:
            - 반드시 JSON 배열만 출력
            - 정확히 5개
            - 문자열만 포함
            - **JSON 배열 형식만** 출력: ["키워드1", "키워드2", ...]
            - 대괄호 [ ]로 시작하고 끝나야 함

            """;

        // AI 사용자 프롬프트 (실제 요청 내용)
        // String.format()으로 동적 값 삽입 (%s는 문자열 플레이스홀더)
        String prompt = String.format("""
            다음 정보를 바탕으로 뉴스 검색에 최적화된 키워드 5개를 생성해 주세요.
            직군: %s
            세부 직무: %s
            
            기존 검증된 키워드 (이 범위와 성격을 유지):
            %s
            
            """, jobGroup,  // 첫 번째 %s
                 jobRole != null ? jobRole : "없음",  // 두 번째 %s
                 referenceKeywordsStr.isEmpty() ? "없음" : referenceKeywordsStr
            );  // 세 번째 %s
                 

        try {
            // Spring AI ChatClient를 사용한 AI 호출
            String aiResult = chatClient.prompt()  // 프롬프트 시작
                .system(system)                     // 시스템 메시지 설정 (AI 역할 정의)
                .user(prompt)                       // 사용자 메시지 설정 (실제 요청)
                .call()                             // AI 모델 호출
                .content();                         // 응답 내용 추출 (String 타입)

            // JSON 배열 문자열을 Java List로 변환
            List<String> keywords = new ArrayList<>();  // 가변(mutable) 리스트 생성
            JSONArray arr = new JSONArray(aiResult);    // org.json 라이브러리로 JSON 파싱
            
            // for 루프로 JSON 배열의 각 요소를 리스트에 추가
            for (int i = 0; i < arr.length(); i++) {
                keywords.add(arr.getString(i));  // i번째 요소를 String으로 추출하여 추가
            }

            // 성공 로그 출력 (생성된 키워드 개수와 참고 키워드 개수 표시)
            log.info("직군 '{}', 직무 '{}' - AI 생성 키워드 {}개 (참고 키워드: {}개)", 
                    jobGroup, jobRole, keywords.size(), referenceKeywords.size());
            
            return keywords;  // 생성된 키워드 리스트 반환

        } catch (Exception e) {  // AI 호출 실패, JSON 파싱 오류 등 모든 예외 처리
            // 에러 로그 출력 (예외 정보 포함)
            log.error("AI 키워드 생성 실패 - 기존 검증된 키워드 사용. 직군: {}, 직무: {}", 
                     jobGroup, jobRole, e);
            
            // ✅ AI 실패 시 기존 검증된 키워드 사용 (1차 fallback)
            if (!referenceKeywords.isEmpty()) {  // 참고 키워드가 존재하면
                return new ArrayList<>(referenceKeywords);  // 새 리스트로 복사하여 반환 (원본 보호)
            }
            
            // ✅ 참고 키워드도 없으면 기본 키워드 반환 (2차 fallback)
            // List.of()로 즉석에서 불변 리스트 생성
            return List.of(
                jobGroup + "",   // 문자열 연결 연산자로 동적 키워드 생성
                jobGroup + "",
                jobGroup + " 업계",
                "기업 전략",          // 모든 직군에 공통으로 유용한 키워드
                "조직개편"
            );
        }
    }
}
