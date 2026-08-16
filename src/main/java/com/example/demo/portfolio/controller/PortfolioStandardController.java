package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.entity.PortfolioStandard;
import com.example.demo.portfolio.service.PortfolioStandardService;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 평가 기준 API 컨트롤러
 * - 프론트엔드에서 평가 기준 정보를 조회하는 API 제공
 */
@RestController
@RequestMapping("/portfolio-standard")
@Slf4j
public class PortfolioStandardController {

    @Autowired
    private PortfolioStandardService portfolioStandardService;

    /**
     * 모든 평가 기준 조회
     * GET /portfolio-standard
     */
    @GetMapping
    public ResponseEntity<List<PortfolioStandard>> getAllStandards() {
        log.info("모든 평가 기준 조회 요청");
        List<PortfolioStandard> standards = portfolioStandardService.getAllStandards();
        return ResponseEntity.ok(standards);
    }

    // 회원별 평가 기준
    @GetMapping("/member")
    public ResponseEntity<List<PortfolioStandard>> getStandardsByMember(@RequestParam("memberId") int memberId) {
        List<PortfolioStandard> standards = portfolioStandardService.getStandardsByMemberId(memberId);
        return ResponseEntity.ok(standards);
    }

    /**
     * 직군 + 직무로 평가 기준 조회
     * /portfolio-standard/by-job?jobGroup=백엔드&jobRole=백엔드
     */
    @GetMapping("/by-job")
    public ResponseEntity<List<PortfolioStandard>> getStandardsByJob(
            @RequestParam("jobGroup") String jobGroup,
            @RequestParam("jobRole") String jobRole) {

        // 직군/직무 정보가 모두 있는 경우
        List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobInfo(jobGroup, jobRole);

        return ResponseEntity.ok(standards);
    }

}
