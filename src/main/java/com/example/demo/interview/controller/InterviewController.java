package com.example.demo.interview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.interview.dto.response.AiQuestionResponse;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.dto.response.CompanySearchResponse;
import com.example.demo.interview.dto.response.InterviewReportResponse;
import com.example.demo.interview.dto.response.SaveSessionResponse;
import com.example.demo.interview.dto.response.SessionFeedbackResponse;
import com.example.demo.interview.dto.response.TotalQuestionResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.example.demo.interview.entity.InterviewQuestion;
import com.example.demo.interview.entity.InterviewSession;
import com.example.demo.interview.service.InterviewService;


@RestController
@RequestMapping("/interview")
public class InterviewController {

    // Service
    @Autowired
    private InterviewService interviewService;

    // 면접 세션 종료 & 종합 피드백 생성========================================================================================================================
    @PostMapping("/final-feedback")
    public ResponseEntity<SessionFeedbackResponse> createInterviewFinalFeedback(@RequestParam("sessionId") int sessionId) throws Exception {
        return ResponseEntity.ok(interviewService.createInterviewFeedback(sessionId));
    }

    // 면접 목록 조회==========================================================================================================================================
    @GetMapping("/list")
    public ResponseEntity<List<InterviewSession>> getInterviewList(@RequestParam("memberId") int memberId) {
        List<InterviewSession> list = interviewService.getInterviewSessions(memberId);
        return ResponseEntity.ok(list);
    }

    // 면접 상세 조회 (리포트 상세보기)==========================================================================================================================
    @GetMapping("/report")
    public ResponseEntity<InterviewReportResponse> getInterviewDetail(@RequestParam("sessionId") int sessionId) throws Exception {
        return ResponseEntity.ok(interviewService.getInterviewReport(sessionId));
    }
    

    // AI 질문 생성============================================================================================================================================
    @PostMapping("/ai-questions")
    public ResponseEntity<List<AiQuestionResponse>> createQuestion(
        @RequestParam("memberId") Integer memberId,
        @RequestParam("type") String type,
        @RequestParam("targetCompany") String targetCompany,
        @RequestParam(value = "keywords", required = false) List<String> keywords,
        @RequestParam(value = "documentFile", required = false) MultipartFile documentFile
    ) throws Exception {

        List<AiQuestionResponse> result = interviewService.createAiQuestion(memberId, type, targetCompany, keywords, documentFile);
        return ResponseEntity.ok(result);
    }   

    // 기업 검색============================================================================================================================================
    @GetMapping("/search")
    public ResponseEntity<List<String>> searchCompany(@RequestParam("query") String query) {
        return ResponseEntity.ok(interviewService.searchCompany(query));
    }

    // 기업의 인재상 요약=====================================================================================================================================
    @GetMapping("/company-ideal")
    public ResponseEntity<CompanySearchResponse> idealTalent(@RequestParam("companyName") String companyName) {
        return ResponseEntity.ok(interviewService.searchCompanyIdealTalent(companyName));
    }
    
    

    // 세션 저장============================================================================================================================================
    @PostMapping("/session-save")
    public ResponseEntity<List<SaveSessionResponse>> saveSession(
        @RequestParam Integer memberId,
        @RequestParam String type,
        @RequestParam String targetCompany,
        @RequestParam(required = false) List<String> keywords,
        @RequestParam(required = false) List<String> aiQuestions,
        @RequestParam(required = false) List<String> customQuestions,
        @RequestParam(required = false) MultipartFile file
    ) throws Exception {

        List<SaveSessionResponse> response = interviewService.saveSessionAndQuestion(memberId, type, targetCompany, keywords, file, aiQuestions, customQuestions);
        return ResponseEntity.ok(response);
    }
    
    // 세션별 질문 조회==============================================================================================================================
    @GetMapping("/questions")
    public ResponseEntity<List<TotalQuestionResponse>> getSessionDetail(@RequestParam("sessionId") Integer sessionId) {
        List<TotalQuestionResponse> response = interviewService.getSessionDetail(sessionId);
        return ResponseEntity.ok(response);
    }

    // 사용자별 질문 조회====================================================================================================================================
    @GetMapping("/all-questions")
    public ResponseEntity<List<InterviewQuestion>> getAllQuestions(@RequestParam("memberId") int memberId) {
        return ResponseEntity.ok(interviewService.getAllQuestionsByMemberId(memberId));
    }
    
    

    // 답변 제출============================================================================================================================================
    @PostMapping(
        value = "/submit-answer",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Integer> submitAnswer(
        @RequestParam("questionId") int questionId,
        @RequestParam(value = "audio", required = false) MultipartFile audio,
        @RequestParam(value = "video", required = false) MultipartFile video
    ) throws Exception {

        return ResponseEntity.ok(interviewService.createInterviewAnswer(questionId, audio, video));        
    }
    
    // 답변 다시 제출============================================================================================================================================
    @PostMapping(
        value = "/re-submit-answer",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Integer> reSubmitAnswer(
        @RequestParam("answerId") int answerId,
        @RequestParam(value = "audio", required = false) MultipartFile audio,
        @RequestParam(value = "video", required = false) MultipartFile video
    ) throws Exception {

        return ResponseEntity.ok(interviewService.modifyInterviewAnswer(answerId, audio, video));        
    }
    
    // 답변별 AI 피드백 생성===================================================================================================================
    @PostMapping(
       value = "/create-feedback",
       consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
       produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnswerFeedbackResponse> createFeedback(
        @RequestParam("answerId") int answerId,
        @RequestParam(value = "audio", required = false) MultipartFile audio,
        @RequestParam(value = "videoAudio", required = false) MultipartFile videoAudio,
        @RequestParam(value = "frames", required = false) List<MultipartFile> frames
    ) throws Exception {
        
        return ResponseEntity.ok(interviewService.createAnswerFeedback(answerId, audio, videoAudio, frames));
    }
    

    // 면접 질문 기반 답변 조회===============================================================================================================
    @GetMapping("/answer-by-question")
    public ResponseEntity<InterviewAnswer> getAnswerByQuestionId(@RequestParam("questionId") int questionId) {
        return ResponseEntity.ok(interviewService.getInterviewAnswersByQuestionId(questionId));
    }

    // 면접 답변 단건 조회===============================================================================================================================
    @GetMapping("/answer-detail")
    public ResponseEntity<InterviewAnswer> getOneAnswer(@RequestParam("answerId") int answerId) {
        return ResponseEntity.ok(interviewService.getOneInterviewAnswer(answerId));
    }
}
