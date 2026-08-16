package com.example.demo.resume.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.resume.dto.request.ResumeCoachRequest;
import com.example.demo.resume.dto.response.ResumeCoachResponse;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.example.demo.resume.service.ResumeService;


@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;
    
    // AI 분석 결과 조회
    @GetMapping("/report/{resumeId}")
    public ResponseEntity<ResumeReportResponse> getReport(
            @PathVariable("resumeId") int resumeId) throws Exception {
        ResumeReportResponse report = resumeService.getResumeReport(resumeId);
        return ResponseEntity.ok(report);
    }
    // 이력서 생성
    @PostMapping("/create")
    public ResponseEntity<Integer> createResume(@RequestBody Resume resume) throws Exception {
        int newId = resumeService.createResume(resume);
        return ResponseEntity.ok(newId);
    } 

    // 이력서 상세 조회
    @GetMapping("/{resumeId}")
    public ResponseEntity<Resume> getResume(@PathVariable("resumeId") int resumeId) {
        return ResponseEntity.ok(resumeService.getResume(resumeId));
    }

    // 이력서 목록 조회
    @GetMapping("/list/{memberId}")
    public ResponseEntity<List<Resume>> getResumeList(@PathVariable("memberId") int memberId) {
        return ResponseEntity.ok(resumeService.getResumeList(memberId));
    }

    // 이력서 수정
    @PutMapping("/update")
    public ResponseEntity<Integer> updateResume(@RequestBody Resume resume) {   
        int result = resumeService.updateResume(resume);
        return ResponseEntity.ok(result);
    }

    // 이력서 삭제
    @DeleteMapping("/delete/{resumeId}")
    public ResponseEntity<Integer> removeResume(@PathVariable("resumeId") int resumeId) {
        return ResponseEntity.ok(resumeService.deleteResume(resumeId));
    }

    // AI 분석
    @PostMapping("/analyze")
    public ResponseEntity<ResumeReportResponse> analyze(
            @RequestParam("memberId") int memberId,
            @RequestParam("resumeId") int resumeId) throws Exception {

        ResumeReportResponse result = resumeService.analyzeResume(memberId, resumeId);
        return ResponseEntity.ok(result);
    }

    // 실시간 코칭
    @PostMapping("/coach")
    public ResponseEntity<ResumeCoachResponse> coach(@RequestBody ResumeCoachRequest request) throws Exception {
        ResumeCoachResponse response = resumeService.coachResponse(request);
        return ResponseEntity.ok(response);
    }

}
