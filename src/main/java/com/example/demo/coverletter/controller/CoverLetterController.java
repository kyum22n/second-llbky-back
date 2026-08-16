package com.example.demo.coverletter.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.coverletter.dto.request.CoverLetterCoachRequest;
import com.example.demo.coverletter.dto.response.CoverLetterCoachResponse;
import com.example.demo.coverletter.dto.response.CoverLetterCreateResponse;
import com.example.demo.coverletter.dto.response.WritingStyle;
import com.example.demo.coverletter.entity.CoverLetter;
import com.example.demo.coverletter.service.CoverLetterService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/coverletter")
public class CoverLetterController {

    // Service
    @Autowired
    private CoverLetterService coverLetterService;

    // 자소서 작성 시 실시간 코칭 받기---------------------------------------------------
    @PostMapping("/realtime-coach")
    public ResponseEntity<CoverLetterCoachResponse> coach(@RequestBody CoverLetterCoachRequest request) { 
        return ResponseEntity.ok(coverLetterService.realtimeCoach(request));        
    }
    

    // 자소서 리포트 생성-------------------------------------------------------------
    @PostMapping("/save")
    public ResponseEntity<CoverLetterCreateResponse> saveCoverLetter(
        @RequestBody CoverLetter coverLetter,
        @RequestParam("memberId") int memberId
    ) throws Exception {
        CoverLetterCreateResponse response = coverLetterService.createCoverLetter(coverLetter, memberId);
        return ResponseEntity.ok(response);
    }

    // 자소서 목록 조회--------------------------------------------------------------
    @GetMapping("/list")
    public ResponseEntity<List<CoverLetter>> getCoverLetterList(@RequestParam("memberId") int memberId) {
        return ResponseEntity.ok(coverLetterService.getAllCoverLetters(memberId));
    }

    // 자소서 상세보기-----------------------------------------------------------------
    @GetMapping("/detail")
    public ResponseEntity<CoverLetter> getCoverLetterDetail(@RequestParam("coverletterId") int coverletterId) {
        return ResponseEntity.ok(coverLetterService.getOneCoverLetter(coverletterId));
    }
    
    // 문체 버전 생성------------------------------------------------------------------------
    @GetMapping("/detail/styles")
    public ResponseEntity<WritingStyle> getWritingStyles(
        @RequestParam("coverletterId") int coverletterId,
        @RequestParam("section") String section
    ) {
        return ResponseEntity.ok(coverLetterService.createStyles(coverletterId, section));
    }

    // 문체 버전 적용--------------------------------------------------------------------------
    @PutMapping("/detail/styles/apply")
    public ResponseEntity<Integer> applyWritingStyles(
        @RequestParam("coverletterId") int coverletterId,
        @RequestParam("memberId") int memberId,
        @RequestParam("section") String section,
        @RequestBody String newContent
    ) {
        return ResponseEntity.ok(coverLetterService.applyStyles(coverletterId, memberId, section, newContent));
    }
    
    // 자소서 수정----------------------------------------------------------------------------
    @PutMapping("/detail/update")
    public ResponseEntity<Integer> updateCoverLetter(
        @RequestParam("memberId") int memberId, 
        @RequestBody CoverLetter coverLetter
    ) {
        return ResponseEntity.ok(coverLetterService.modifyCoverLetter(coverLetter, memberId));
    }

    // 자소서 삭제--------------------------------------------------------------------------
    @DeleteMapping("/delete")
    public ResponseEntity<Integer> deleteCoverLetter(
        @RequestParam("memberId") int memberId,
        @RequestParam("coverletterId") int coverletterId
    ) {
        return ResponseEntity.ok(coverLetterService.removeCoverLetter(coverletterId, memberId));
    }

}
