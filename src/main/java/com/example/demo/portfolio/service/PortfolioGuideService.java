package com.example.demo.portfolio.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.ai.portfolioguide.PortfolioGuideAgent;
import com.example.demo.ai.portfolioguide.PortfolioGuideAgentDirect;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.GuideContentData;
import com.example.demo.portfolio.dto.GuideItemData;
import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.GuideStepData;
import com.example.demo.portfolio.dto.request.GuideProgressSaveRequest;
import com.example.demo.portfolio.dto.request.GuideRequest;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.entity.PortfolioStandard;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PortfolioGuideService {

    @Autowired
    private PortfolioGuideDao portfolioGuideDao;
    @Autowired
    private PortfolioGuideAgent portfolioGuideAgent;
    @Autowired
    private PortfolioGuideAgentDirect portfolioGuideAgentDirect;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private PortfolioStandardDao portfolioStandardDao;

    @Autowired
    private PortfolioGuidePdfService pdfService;

    // 메인 코칭 메서드
    public GuideResult provideCoaching(GuideRequest request) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();

        log.info("코칭 요청 시작 - memberId: {}, inputFieldType: {}, userInput: {}",
                request.getMemberId(), request.getInputFieldType(), request.getUserInput());

        // 1. Member 조회
        Member member = memberDao.findById(request.getMemberId());
        if (member == null) {
            throw new NoSuchElementException("존재하지 않는 회원입니다: " + request.getMemberId());
        }

        // 2. PortfolioStandard 조회
        List<PortfolioStandard> standards = loadStandards(request, member);

        // 3. AI 에이전트로 코칭 수행
        GuideResult result = portfolioGuideAgent.evaluate(request, member, standards);

        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("AI 코칭 완료 - 처리시간: {}ms", duration.toMillis());

        // 4. 가이드 ID가 있으면 피드백 저장
        Integer guideId = request.getGuideId();
        if (guideId != null) {
            saveFeedbackToDatabase(guideId, result);
            log.info("피드백 저장 완료 - guideId: {}", guideId);
        }

        return result;
    }

    // 직무별 표준 가이드라인 조회
    private List<PortfolioStandard> loadStandards(GuideRequest request, Member member) {
        List<PortfolioStandard> standards = null;

        // 1. standardId가 지정되면 해당 표준만 조회
        if (request.getStandardId() != null) {
            PortfolioStandard standard = portfolioStandardDao.selectStandardById(request.getStandardId());
            if (standard != null) {
                standards = List.of(standard);
                log.info("특정 표준 적용: {} (ID: {})", standard.getStandardName(), request.getStandardId());
            }
        }

        // 2. 없으면 직군/직무별 가이드라인 사용
        if (standards == null || standards.isEmpty()) {
            standards = portfolioStandardDao.selectStandardsByJobInfo(
                    member.getJobGroup(),
                    member.getJobRole());
            log.info("직군/직무별 표준 적용: {} {}개", member.getJobGroup(), standards != null ? standards.size() : 0);
        }

        // 3. 그것도 없으면 전체 표준 사용
        if (standards == null || standards.isEmpty()) {
            standards = portfolioStandardDao.selectAllStandards();
            log.warn("기본 표준 적용: 전체 {}개", standards != null ? standards.size() : 0);
        }

        return standards;
    }

    // 가이드 ID로 조회
    public PortfolioGuide getGuideById(Integer guideId) {

        return portfolioGuideDao.selectGuideById(guideId);
    }

    /**
     * 회원별 가이드 목록 조회
     */
    public List<PortfolioGuide> getGuidesByMemberId(Integer memberId) {

        return portfolioGuideDao.selectGuidesByMemberId(memberId);
    }

    // 저장된 피드백 조회
    public GuideResult getGuideFeedback(Integer guideId) throws com.fasterxml.jackson.core.JsonProcessingException {

        PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);
        if (guide == null) {
            throw new NoSuchElementException("존재하지 않는 가이드입니다: " + guideId);
        }

        String feedbackJson = guide.getGuideFeedback();
        if (feedbackJson == null || feedbackJson.trim().isEmpty()) {
            throw new NoSuchElementException("저장된 피드백이 없습니다: " + guideId);
        }

        // JSONB → GuideResult 객체 변환
        GuideResult feedback = objectMapper.readValue(feedbackJson, GuideResult.class);
        log.info("피드백 조회 성공 - 점수: {}", feedback.getAppropriatenessScore());

        return feedback;
    }

    // AI 피드백을 JSONB로 저장
    @Transactional
    private void saveFeedbackToDatabase(Integer guideId, GuideResult feedback)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        log.info("피드백 저장 시작 - guideId: {}", guideId);

        // GuideResult → JSON 문자열 변환
        String feedbackJson = objectMapper.writeValueAsString(feedback);

        // DB 업데이트를 위한 파라미터 맵 생성
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("guideId", guideId);
        updateParams.put("guideFeedback", feedbackJson);

        // DB 업데이트 실행
        int updated = portfolioGuideDao.updateGuideFeedback(updateParams);

        if (updated == 0) {
            throw new IllegalStateException("가이드를 찾을 수 없습니다: " + guideId);
        }

        log.info("피드백 저장 완료 - guideId: {}, 업데이트된 행: {}", guideId, updated);
    }

    // 새 가이드 생성
    @Transactional // 트랜잭션 처리: DB 작업 보장
    public PortfolioGuide createGuide(GuideRequest request) throws Exception {
        // INFO 로그: 가이드 생성 시작 시점 기록
        log.info("가이드 생성 - memberId: {}, title: {}",
                request.getMemberId(), request.getTitle());

        // 회원 정보 검증 (선택사항)
        Member member = memberDao.findById(request.getMemberId());
        if (member == null)
            throw new NoSuchElementException("회원 없음");

        // ========== 가이드 엔티티 생성 ==========
        PortfolioGuide guide = new PortfolioGuide();
        guide.setMemberId(request.getMemberId()); // 회원 ID 설정
        // 제목이 없으면 기본 제목 사용 (null 안전 처리)
        guide.setTitle(request.getTitle() != null ? request.getTitle() : "새 포트폴리오 가이드");
        guide.setStandardId(request.getStandardId()); // 평가 기준 ID 설정

        // ========== 초기 상태 설정 ==========
        guide.setCompletionPercentage(0); // 진행률 0%로 시작
        guide.setIsCompleted(false); // 미완료 상태
        guide.setCurrentStep(1); // 1단계부터 시작
        guide.setTotalSteps(5); // 전체 5단계 구조

        // ========== 초기 빈 가이드 구조 생성 ==========
        // 5단계 구조를 가진 빈 가이드 데이터 생성
        GuideContentData initialContent = createEmptyGuideStructure();
        // Java 객체 → JSON 문자열 변환 (DB JSONB 컬럼에 저장)
        String contentJson = objectMapper.writeValueAsString(initialContent);
        guide.setGuideContent(contentJson); // guide_content 필드 설정

        // guide_feedback는 NULL로 시작 (필요시에만 저장)
        guide.setGuideFeedback(null);

        // ========== DB 저장 ==========
        // MyBatis Dao를 통해 INSERT 실행
        int result = portfolioGuideDao.insertGuide(guide);

        // 저장 실패 시 예외 발생 (result == 0이면 영향받은 행 없음)
        if (result == 0) {
            throw new IllegalStateException("가이드 생성에 실패했습니다.");
        }

        // 생성 완료 로그 (DB에서 자동 생성된 guideId 포함)
        log.info("가이드 생성 완료 - guideId: {}", guide.getGuideId());

        return guide; // 생성된 가이드 엔티티 반환
    }

    // 가이드 저장 (프론트엔드 계산 값 사용)
    @Transactional // 트랜잭션 처리
    public PortfolioGuide saveGuide(GuideProgressSaveRequest request) throws Exception {
        PortfolioGuide existingGuide = portfolioGuideDao.selectGuideById(request.getGuideId());
        if (existingGuide == null) {
            throw new IllegalArgumentException("존재하지 않는 가이드입니다: " + request.getGuideId());
        }

        GuideContentData contentData = new GuideContentData();
        contentData.setSteps(request.getGuideSteps()); // ← 이게 List<GuideStepData>임
        contentData.setLastUpdated(LocalDateTime.now().toString());
        String contentJson = objectMapper.writeValueAsString(contentData);

        int completionPercentage = request.getCompletionPercentage() != null
                ? request.getCompletionPercentage()
                : 0;

        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("guideId", request.getGuideId());
        updateParams.put("guideContent", contentJson);
        updateParams.put("completionPercentage", completionPercentage);
        updateParams.put("currentStep", request.getCurrentStep());
        updateParams.put("isCompleted", request.getCompletionPercentage() == 100);

        int result = portfolioGuideDao.updateGuideProgress(updateParams);

        if (result == 0) {
            throw new IllegalStateException("가이드 업데이트에 실패했습니다.");
        }

        log.info("가이드 저장 완료 - 진행률: {}%", request.getCompletionPercentage());

        return portfolioGuideDao.selectGuideById(request.getGuideId());

    }

    // 실시간 AI 코칭
    public GuideResult getRealtimeCoaching(GuideRequest request) throws Exception {
        // INFO 로그: AI 코칭 요청 시작
        log.info("AI 코칭 요청 - memberId: {}, fieldType: {}",
                request.getMemberId(), request.getInputFieldType());
        // ========== AI 에이전트 호출 ==========
        GuideResult result = provideCoaching(request);
        return result; // AI 코칭 결과 반환
    }

    // 저장된 AI 피드백 조회
    public GuideResult getStoredFeedback(Integer guideId) throws Exception {
        // ========== 가이드 조회 ==========
        PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);

        // 가이드 존재 여부 확인
        if (guide == null) {
            throw new IllegalArgumentException("존재하지 않는 가이드입니다: " + guideId);
        }

        // ========== 저장된 피드백 확인 ==========
        String feedbackJson = guide.getGuideFeedback();

        // 피드백이 없거나 빈 문자열이면 예외 발생
        if (feedbackJson == null || feedbackJson.trim().isEmpty()) {
            return null; // 저장된 피드백 없음 (null 반환)
        }

        // ========== JSON 문자열 → GuideResult 객체 변환 ==========
        // ObjectMapper로 역직렬화 (JSON → Java 객체)
        return objectMapper.readValue(feedbackJson, GuideResult.class);
    }

    // 빈 가이드 구조 생성 (5단계)
    private GuideContentData createEmptyGuideStructure() {
        GuideContentData content = new GuideContentData();
        List<GuideStepData> steps = new ArrayList<>();

        // ========== 5단계 제목 정의 ==========
        String[] stepTitles = {
                "프로젝트 개요", // 1단계
                "핵심 역량 & 기술", // 2단계
                "주요 성과", // 3단계
                "문제 해결", // 4단계
                "향후 계획" // 5단계
        };

        // ========== 각 단계별 GuideStepData 생성 ==========
        for (int i = 0; i < 5; i++) {
            GuideStepData step = GuideStepData.builder()
                    .stepNumber(i + 1) // 단계 번호 (1-5)
                    .stepTitle(stepTitles[i]) // 단계 제목
                    .stepProgress(0) // 초기 진행률 0%
                    .items(createEmptyStepItems(stepTitles[i])) // 단계별 빈 항목 생성
                    .build();
            steps.add(step); // 단계 리스트에 추가
        }

        // ========== GuideContentData 설정 ==========
        content.setSteps(steps); // 5개 단계 설정
        content.setLastUpdated(LocalDateTime.now().toString()); // 마지막 업데이트 시각

        return content; // 초기 가이드 구조 반환
    }

    // 단계별 빈 항목들 생성
    private List<GuideItemData> createEmptyStepItems(String stepTitle) {
        List<GuideItemData> items = new ArrayList<>();

        // ========== 단계별로 다른 항목들 생성 ==========
        // switch-case로 단계 제목에 따라 다른 항목 생성
        switch (stepTitle) {
            case "프로젝트 개요":
                // 1단계: 프로젝트 기본 정보
                items.add(createEmptyItem("프로젝트명", "프로젝트의 제목을 입력하세요"));
                items.add(createEmptyItem("개발 기간", "프로젝트 진행 기간을 입력하세요"));
                items.add(createEmptyItem("프로젝트 목적", "프로젝트의 목표와 배경을 설명하세요"));
                break;

            case "핵심 역량 & 기술":
                // 2단계: 기술스택과 역할
                items.add(createEmptyItem("주요 기술스택", "사용한 기술들을 나열하세요"));
                items.add(createEmptyItem("담당 역할", "프로젝트에서의 역할을 설명하세요"));
                break;

            default:
                // 나머지 단계: 기본 항목 생성
                items.add(createEmptyItem(stepTitle + " 항목 1", "내용을 입력하세요"));
                break;
        }

        return items; // 생성된 항목 리스트 반환
    }

    // 빈 가이드 항목 생성
    private GuideItemData createEmptyItem(String title, String placeholder) {
        GuideItemData item = new GuideItemData();
        item.setTitle(title); // 항목 제목 설정
        item.setContent(""); // 빈 내용 (사용자가 입력 예정)
        item.setStatus("미작성"); // 초기 상태: "미작성"
        item.setFeedback(null); // AI 피드백은 실시간으로만 제공 (저장 안함)
        return item;
    }

    // 가이드 PDF를 생성해 응답 스트림으로 내려보내는 서비스
    public void generatePdf(PortfolioGuide guide, HttpServletResponse response)
            throws Exception {
        // INFO 로그: PDF 생성 요청
        log.info("PDF 생성 요청 - guideId: {}", guide.getGuideId());

        // ========== 실제 구현은 PortfolioGuidePdfService 사용 ==========
        // iText 기반 PDF 생성 로직
        pdfService.generateGuidePdf(guide, response);

    }

    // 회원별 전체 가이드 PDF 생성
    public void generateMemberPdf(List<PortfolioGuide> guides,
            HttpServletResponse response) throws Exception {
        // INFO 로그: 회원별 PDF 생성 요청
        log.info("회원별 PDF 생성 요청 - 가이드 수: {}", guides.size());

        // ========== 실제 구현은 PortfolioGuidePdfService 사용 ==========
        pdfService.generateMemberGuidesPdf(guides, response);

    }

    public int deleteGuide(int memberId) {
        int result = portfolioGuideDao.deleteAllGuides(memberId);
        return result;
    }

    // 특정 가이드 삭제
    public int deleteGuideById(int guideId, int memberId) {
        int result = portfolioGuideDao.deleteGuideById(guideId, memberId);

        return result;
    }

    // Direct 방식 테스트용
    public GuideResult provideCoachingDirect(GuideRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("코칭 요청 (Direct) - memberId: {}, inputFieldType: {}",
                request.getMemberId(), request.getInputFieldType());

        // 1. Member 조회
        Member member = memberDao.findById(request.getMemberId());
        if (member == null) {
            throw new NoSuchElementException("존재하지 않는 회원입니다: " + request.getMemberId());
        }

        GuideResult result = portfolioGuideAgentDirect.evaluateDirect(request, member);

        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("AI 코칭 완료 (Direct) - 처리시간: {}ms", duration.toMillis());

        return result;
    }

}