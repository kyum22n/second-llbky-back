package com.example.demo.interview.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.interview.AnswerFeedbackAgent;
import com.example.demo.ai.interview.CompanyIdealTalentAgent;
import com.example.demo.ai.interview.CompanySearchAgent;
import com.example.demo.ai.interview.CreateQuestionAgent;
import com.example.demo.ai.interview.InterviewFeedbackAgent;
import com.example.demo.ai.interview.STTAgent;
import com.example.demo.ai.interview.VisualAnalysisAgent;
import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.request.QuestionRequest;
import com.example.demo.interview.dto.response.AiQuestionResponse;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.dto.response.CompanySearchResponse;
import com.example.demo.interview.dto.response.InterviewQAResponse;
import com.example.demo.interview.dto.response.InterviewReportResponse;
import com.example.demo.interview.dto.response.SaveSessionResponse;
import com.example.demo.interview.dto.response.SessionFeedbackResponse;
import com.example.demo.interview.dto.response.TotalQuestionResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.example.demo.interview.entity.InterviewQuestion;
import com.example.demo.interview.entity.InterviewSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterviewService {
    // DAO
    @Autowired
    private InterviewSessionDao interviewSessionDao;
    @Autowired
    private InterviewQuestionDao interviewQuestionDao;
    @Autowired
    private InterviewAnswerDao interviewAnswerDao;

    // AI Agent
    @Autowired
    private CreateQuestionAgent createQuestionAgent;
    @Autowired
    private STTAgent sttAgent;
    @Autowired
    private VisualAnalysisAgent visualAnalysisAgent;
    @Autowired
    private AnswerFeedbackAgent answerFeedbackAgent;
    @Autowired
    private CompanySearchAgent companySearchAgent;
    @Autowired
    private CompanyIdealTalentAgent companyIdealTalentAgent;
    @Autowired
    private InterviewFeedbackAgent interviewFeedbackAgent;

    // ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;

    /*
     * ====================
     * 면접 세션 관련 메소드
     * =====================
     */

    // 면접 세션 종료 & 종합 피드백 생성====================================================================================
    public SessionFeedbackResponse createInterviewFeedback(int sessionId) throws Exception {
        // AI Agent 호출
        return interviewFeedbackAgent.execute(sessionId);
    }

    // 면접 목록 조회=====================================================================================================
    public List<InterviewSession> getInterviewSessions(int memberId) {
        return interviewSessionDao.selectAllInterviewSessions(memberId);
    }

    // 면접 상세 조회 (리포트 상세보기)=====================================================================================
    public InterviewReportResponse getInterviewReport(int sessionId) throws Exception {

        // 1) 세션 정보 조회
        InterviewSession session = interviewSessionDao.selectOneInterviewSession(sessionId);
        if (session == null) {
            throw new RuntimeException("Interview session not found");
        }

        // 2) 종합 피드백 불러오기
        SessionFeedbackResponse finalFeedback = null;

        if (session.getReportFeedback() != null) {
            finalFeedback = objectMapper.readValue(
                    session.getReportFeedback(),
                    SessionFeedbackResponse.class);
        }

        // 3) 질문 + 답변 조회
        // 질문 목록 얻기
        List<InterviewQuestion> questions = interviewQuestionDao.selectInterviewQuestionsBySessionId(sessionId);
        // 질문 - 답변 목록 얻기
        List<InterviewQAResponse> qaList = new ArrayList<>();

        for (InterviewQuestion q : questions) {
            // 질문 ID를 통해 답변 얻기
            InterviewAnswer answer = interviewAnswerDao.selectInterviewAnswerByQuestionId(q.getQuestionId());
        
            InterviewQAResponse qaResponse = new InterviewQAResponse();
            qaResponse.setQuestionId(q.getQuestionId());
            qaResponse.setQuestionText(q.getQuestionText());

            if (answer != null) {
                qaResponse.setAnswerId(answer.getAnswerId());
                qaResponse.setAnswerText(answer.getAnswerText());
                
                if (answer.getAnswerFeedback() != null) {
                    // JSON 문자열 -> 객체로 변환
                    AnswerFeedbackResponse feedbackObj = objectMapper.readValue(answer.getAnswerFeedback(), AnswerFeedbackResponse.class);
                    qaResponse.setAnswerFeedback(feedbackObj);
                }

                // 답변 원본 파일(음성/영상)
                qaResponse.setAudioFileName(answer.getAudioFileName());
                qaResponse.setAudioFileType(answer.getAudioFileType());
                qaResponse.setAudioFileData(answer.getAudioFileData());
                qaResponse.setVideoFileName(answer.getVideoFileName());
                qaResponse.setVideoFileType(answer.getVideoFileType());
                qaResponse.setVideoFileData(answer.getVideoFileData());
            }

            qaList.add(qaResponse);

        }

        // 면접 상세 정보 얻기
        InterviewReportResponse response = new InterviewReportResponse();
        response.setSessionInfo(session);   // 면접 기본 정보 (유형, 희망 기업 등)
        response.setFinalFeedback(finalFeedback);   // 종합 피드백
        response.setQaList(qaList); // 질문-답변 리스트

        return response;
    }

    // 면접 세션 질문 조회=================================================================================================
    public List<TotalQuestionResponse> getSessionDetail(Integer sessionId) {
        // 질문 목록 조회
        List<InterviewQuestion> question = interviewQuestionDao.selectInterviewQuestionsBySessionId(sessionId);

        List<TotalQuestionResponse> result = new ArrayList<>();
        for (InterviewQuestion q : question) {
            TotalQuestionResponse dto = new TotalQuestionResponse();
            dto.setQuestionId(q.getQuestionId());
            dto.setQuestionText(q.getQuestionText());
            result.add(dto);
        }

        return result;
    }

    /*
     * ====================
     * 면접 질문 관련 메소드
     * =====================
     */

    // AI 면접 질문 생성==========================================================================
    public List<AiQuestionResponse> createAiQuestion(Integer memberId, String type, String targetCompany,
            List<String> keywords, MultipartFile documentFile) throws Exception {

        QuestionRequest request = new QuestionRequest();
        request.setMemberId(memberId);
        request.setType(type);
        request.setTargetCompany(targetCompany);
        request.setKeywords(keywords);

        if (documentFile != null && !documentFile.isEmpty()) {
            request.setDocumentFileData(documentFile.getBytes());
            request.setDocumentFileName(documentFile.getOriginalFilename());
            request.setDocumentFileType(documentFile.getContentType());
        }

        // Agent 호출
        List<AiQuestionResponse> questionList = createQuestionAgent.createQuestion(request);

        return questionList;
    }

    // 기업 검색=====================================================================================
    public List<String> searchCompany(String query) {
        return companySearchAgent.searchCompanyNames(query);
    }

    // 사용자가 선택한 기업의 인재상 검색 및 요약
    public CompanySearchResponse searchCompanyIdealTalent(String companyName) {
        CompanySearchResponse response = companyIdealTalentAgent.searchCompanyIdealTalent(companyName);
        return response;
    }

    // DB에 면접 질문 저장===========================================================================
    public List<SaveSessionResponse> saveSessionAndQuestion(Integer memberId, String type, String targetCompany,
            List<String> keywords, MultipartFile file,
            List<String> aiQuestions, List<String> customQuestions) throws Exception {

        InterviewSession session = new InterviewSession();
        session.setMemberId(memberId);
        session.setInterviewType(type);
        session.setTargetCompany(targetCompany);
        if (file != null) {
            session.setDocumentFileName(file.getOriginalFilename());
            session.setDocumentFileType(file.getContentType());
            session.setDocumentFileData(file.getBytes());
        }

        // 세션 저장
        interviewSessionDao.insertInterviewSession(session);
        Integer sessionId = session.getSessionId();
        log.info("sessionId : {}", sessionId);

        // AI 질문과 사용자 질문 합치기
        List<String> finalQuestions = new ArrayList<>();
        if (aiQuestions != null)
            finalQuestions.addAll(aiQuestions);
        if (customQuestions != null)
            finalQuestions.addAll(customQuestions);

        // 질문 저장
        List<SaveSessionResponse> responseList = new ArrayList<>();

        for (String q : finalQuestions) {
            InterviewQuestion question = new InterviewQuestion();
            question.setSessionId(sessionId);
            question.setQuestionText(q);
            interviewQuestionDao.insertInterviewQuestion(question);

            // 저장된 내용을 확인하기 위한 반환값
            SaveSessionResponse dto = new SaveSessionResponse();
            dto.setSessionId(sessionId);
            dto.setQuestionId(question.getQuestionId());
            dto.setQuestionText(question.getQuestionText());
            responseList.add(dto);
        }

        return responseList;

    }

    // 사용자별 질문 목록 조회========================================================================================================
    public List<InterviewQuestion> getAllQuestionsByMemberId(int memberId) {
        return interviewQuestionDao.selectAllInterviewQuestions(memberId);
    }

    /*
     * ====================
     * 면접 답변 관련 메소드
     * =====================
     */

    // 답변 제출=======================================================================================
    public int createInterviewAnswer(
            int questionId,
            MultipartFile audio,
            MultipartFile video) throws Exception {

        // DB에 답변 원본 파일 저장
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(questionId);

        if (audio != null && !audio.isEmpty()) {
            answer.setAudioFileName(audio.getOriginalFilename());
            answer.setAudioFileType(audio.getContentType());
            answer.setAudioFileData(audio.getBytes());
        } else if (video != null && !video.isEmpty()) {
            answer.setVideoFileName(video.getOriginalFilename());
            answer.setVideoFileType(video.getContentType());
            answer.setVideoFileData(video.getBytes());
        }

        interviewAnswerDao.insertInterviewAnswer(answer);

        // 답변 ID 반환
        return answer.getAnswerId();
    }

    // 답변 다시 제출===================================================================================
    public int modifyInterviewAnswer(int answerId, MultipartFile audio, MultipartFile video) throws Exception {

        // DB에 답변 원본 파일 업데이트
        InterviewAnswer answer = interviewAnswerDao.selectOneAnswer(answerId);
        if (answer == null) {
            throw new RuntimeException("Answer not found");
        }

        if (audio != null && !audio.isEmpty()) {
            answer.setAudioFileName(audio.getOriginalFilename());
            answer.setAudioFileType(audio.getContentType());
            answer.setAudioFileData(audio.getBytes());
        } else if (video != null && !video.isEmpty()) {
            answer.setVideoFileName(video.getOriginalFilename());
            answer.setVideoFileType(video.getContentType());
            answer.setVideoFileData(video.getBytes());
        }

        return interviewAnswerDao.updateInterviewAnswer(answer);
    }

    // 답변 분석 + 피드백 생성===========================================================================
    @Transactional
    public AnswerFeedbackResponse createAnswerFeedback(
            int answerId,
            MultipartFile audio,
            MultipartFile videoAudio,
            List<MultipartFile> frames) throws Exception {

        log.info("=== Frames received: {} ===", frames != null ? frames.size() : -1);

        // 1. DB에 저장된 답변 조회
        InterviewAnswer answer = interviewAnswerDao.selectOneAnswer(answerId);
        if (answer == null) {
            throw new RuntimeException("Answer not found");
        }

        // 2. STT Agent 호출 - 변환된 텍스트 얻기

        // 변환할 파일
        byte[] sttBytes = null;
        String fileName = null;

        if (videoAudio != null && !videoAudio.isEmpty()) {
            log.info("=== [STT INPUT - videoAudio] ===");
            log.info("Content-Type: {}", videoAudio.getContentType());
            log.info("Filename: {}", videoAudio.getOriginalFilename());
            log.info("Size (bytes): {}", videoAudio.getSize());

            sttBytes = videoAudio.getBytes(); // 영상 오디오 파일
            fileName = videoAudio.getOriginalFilename();

        } else if (audio != null && !audio.isEmpty()) {
            log.info("=== [STT INPUT - audio] ===");
            log.info("Content-Type: {}", audio.getContentType());
            log.info("Filename: {}", audio.getOriginalFilename());
            log.info("Size (bytes): {}", audio.getSize());

            sttBytes = audio.getBytes(); // 순수 음성 파일
            fileName = audio.getOriginalFilename();

        } else {
            log.warn("=== [STT INPUT] No audio/videoAudio file received ===");

            sttBytes = null;
        }

        // 변환된 텍스트
        String answerText = "음성 입력이 감지되지 않았습니다.";
        // 영상에서 추출한 오디오일 경우
        if (videoAudio != null && !videoAudio.isEmpty()) {
            answerText = sttAgent.sttSave(
                    answerId,
                    videoAudio.getOriginalFilename(),
                    videoAudio.getBytes());
        } else if (audio != null && !audio.isEmpty()) {
            // 순수 음성일 경우
            answerText = sttAgent.sttSave(
                    answerId,
                    audio.getOriginalFilename(),
                    audio.getBytes());
        }
        
        // 3. VisualAnalysis Agent 호출 - 이미지 프레임 분석 리스트 얻기
        List<String> visualFeedback = visualAnalysisAgent.analyzeFrames(frames);

        // 4. AnswerFeedback Agent 호출 - 답변별 피드백 생성
        AnswerFeedbackResponse answerFeedback = answerFeedbackAgent.execute(answerId, answerText, visualFeedback);

        return answerFeedback;
    }

    // 면접 질문 선택 시 해당하는 답변 조회===================================================================
    public InterviewAnswer getInterviewAnswersByQuestionId(int questionId) {
        return interviewAnswerDao.selectInterviewAnswerByQuestionId(questionId);
    }

    // 답변 ID로 답변 조회==================================================================================
    public InterviewAnswer getOneInterviewAnswer(int answerId) {
        return interviewAnswerDao.selectOneAnswer(answerId);
    }

}
