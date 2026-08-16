package com.example.demo.coverletter.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.coverletter.CreateWritingStyleAgent;
import com.example.demo.ai.coverletter.FinalFeedbackAgent;
import com.example.demo.ai.coverletter.RealtimeCoachingAgent;
import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.dto.request.CoverLetterCoachRequest;
import com.example.demo.coverletter.dto.response.CoverLetterCoachResponse;
import com.example.demo.coverletter.dto.response.CoverLetterCreateResponse;
import com.example.demo.coverletter.dto.response.CoverLetterFinalFeedback;
import com.example.demo.coverletter.dto.response.WritingStyle;
import com.example.demo.coverletter.entity.CoverLetter;

@Service
public class CoverLetterService {

    // DAO
    @Autowired
    private CoverLetterDao coverLetterDao;

    // AI Agent
    @Autowired
    private FinalFeedbackAgent finalFeedbackAgent;
    @Autowired
    private CreateWritingStyleAgent createWritingStyleAgent;
    @Autowired
    private RealtimeCoachingAgent realtimeCoachingAgent;

    // 자소서 작성 시 실시간 코칭 받기 -------------------------------------------------------------------------------------------
    public CoverLetterCoachResponse realtimeCoach(CoverLetterCoachRequest request) {
        // AI Agent 호출
        return realtimeCoachingAgent.execute(request);
    }


    // 자소서 리포트 생성 --------------------------------------------------------------------------------------------------
    public CoverLetterCreateResponse createCoverLetter(CoverLetter coverLetter, int memberId) throws Exception {

        // 1. 자소서 저장
        coverLetter.setMemberId(memberId);
        // insert
        coverLetterDao.insertCoverLetter(coverLetter);

        // 2. 피드백 생성
        // AI Agent 호출
        CoverLetterFinalFeedback finalFeedback = finalFeedbackAgent.execute(coverLetter.getCoverletterId());

        // 응답 dto 반환
        CoverLetterCreateResponse response = new CoverLetterCreateResponse();
        response.setCoverletterId(coverLetter.getCoverletterId());
        response.setFinalFeedback(finalFeedback);

        return response;
    }


    // 자소서 목록 조회 --------------------------------------------------------------------------------------------
    public List<CoverLetter> getAllCoverLetters(int memberId) {
        return coverLetterDao.selectAllCoverLetters(memberId);
    }


    // 자소서 상세보기 --------------------------------------------------------------------------------------------
    public CoverLetter getOneCoverLetter(int coverletterId) {
        CoverLetter coverLetter = coverLetterDao.selectOneCoverLetter(coverletterId);
        if (coverLetter == null) throw new RuntimeException("CoverLetter not found");

        return coverLetter;
    }
    

    // 자소서 문체 버전 생성하기-----------------------------------------------------------------------------
    public WritingStyle createStyles(int coverletterId, String section) {   
        // AI Agent 호출
        return createWritingStyleAgent.execute(coverletterId, section);
    }


    // 자소서 문체 버전 적용하기-----------------------------------------------------------------------------
    public int applyStyles(int coverletterId, int memberId, String section, String newContent) {

        // 1. 자소서 원본 불러오기
        CoverLetter origin = coverLetterDao.selectOneCoverLetter(coverletterId);
        if (origin == null) {
            throw new RuntimeException("CoverLetter not found");
        }

        if (!origin.getMemberId().equals(memberId)) {
            throw new RuntimeException("Member not found");
        }

        // 2. 버전 적용하기
        origin.setMemberId(memberId);
        // 선택된 section에 새 내용 적용
        switch (section) {
            case "supportMotive":
                origin.setSupportMotive(newContent);
                break;
            case "growthExperience":
                origin.setGrowthExperience(newContent);
                break;
            case "jobCapability":
                origin.setJobCapability(newContent);
                break;
            case "futurePlan":
                origin.setFuturePlan(newContent);
                break;
            default:
                throw new IllegalArgumentException("Invalid section");
        }

        return coverLetterDao.updateCoverLetter(origin);
    }


    // 자소서 수정하기 --------------------------------------------------------------------------------------------
    public int modifyCoverLetter(CoverLetter coverLetter, int memberId) {
        CoverLetter origin = coverLetterDao.selectOneCoverLetter(coverLetter.getCoverletterId());
        if (origin == null) throw new RuntimeException("CoverLetter not found");

        if (!origin.getMemberId().equals(coverLetter.getMemberId())) {
            throw new RuntimeException("Member not found");
        }

        coverLetter.setMemberId(memberId);

        return coverLetterDao.updateCoverLetter(coverLetter);
    }


    //  자소서 삭제하기 ------------------------------------------------------------------------------------------
    public int removeCoverLetter(int coverLetterId, int memberId) {
        int row = coverLetterDao.deleteCoverLetter(coverLetterId, memberId);
        if (row == 0) {
            throw new RuntimeException("Delete failed");
        }

        return row;
    }
}
