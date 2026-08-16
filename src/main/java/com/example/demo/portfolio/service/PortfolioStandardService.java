package com.example.demo.portfolio.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.entity.PortfolioStandard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 평가 기준 서비스
 * - 평가 기준 조회 관련 비즈니스 로직
 */
@Service

@Slf4j
public class PortfolioStandardService {

    @Autowired
    private PortfolioStandardDao portfolioStandardDao;

    @Autowired
    private MemberDao memberDao;

    /**
     * 모든 평가 기준 조회
     */
    public List<PortfolioStandard> getAllStandards() {
        log.info("모든 평가 기준 조회");
        return portfolioStandardDao.selectAllStandards();
    }

    /**
     * ID로 평가 기준 조회
     */
    public PortfolioStandard getStandardById(int standardId) {
        log.info("평가 기준 조회 - standardId: {}", standardId);
        return portfolioStandardDao.selectStandardById(standardId);
    }

    // 회원 id 로 평가 기준 조회
    public List<PortfolioStandard> getStandardsByMemberId(int memberId) {
        Member member = memberDao.findById(memberId);
        if (member == null) {
            log.warn("존재하지 않는 회원 - memberId: {}", memberId);
            throw new RuntimeException("회원을 찾을 수 없습니다.");
        }

        String jobGroup = member.getJobGroup();
        String jobRole = member.getJobRole();

        log.info("조회된 회원 정보 - jobGroup: {}, jobRole: {}", jobGroup, jobRole);

        // 기존 메서드 재사용
        return getStandardsByJobInfo(jobGroup, jobRole);
    }

    /**
     * 직군 + 직무로 평가 기준 조회
     */
    public List<PortfolioStandard> getStandardsByJobInfo(String jobGroup, String jobRole) {
        log.info("직군/직무별 평가 기준 조회 - jobGroup: {}, jobRole: {}", jobGroup, jobRole);

        if (jobGroup == null || jobGroup.trim().isEmpty()) {
            log.warn("직군 정보가 비어있음 - 전체 기준 반환");
            return getAllStandards();
        }

        if (jobRole == null || jobRole.trim().isEmpty()) {
            log.warn("직무 정보가 비어있음 - 전체 기준 반환");
            return getAllStandards();
        }

        List<PortfolioStandard> result = portfolioStandardDao.selectStandardsByJobInfo(jobGroup, jobRole);
        log.info("조회 결과 - 개수: {}", result != null ? result.size() : 0);

        return result;
    }
}
