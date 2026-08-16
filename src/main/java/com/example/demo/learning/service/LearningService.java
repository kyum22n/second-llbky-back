package com.example.demo.learning.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.learning.CreateRoadmapAgent;
import com.example.demo.ai.learning.RecommendSkillAgent;
import com.example.demo.ai.learning.RefineRoadmapAgent;
import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.entity.CoverLetter;
import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.dao.LearningWeekDao;
import com.example.demo.learning.dto.request.RoadmapRefineRequest;
import com.example.demo.learning.dto.request.RoadmapRequest;
import com.example.demo.learning.dto.response.AiCreateDayResponse;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;
import com.example.demo.learning.dto.response.AiCreateWeekResponse;
import com.example.demo.learning.dto.response.DayDetailResponse;
import com.example.demo.learning.dto.response.LearningDetailResponse;
import com.example.demo.learning.dto.response.LearningResponse;
import com.example.demo.learning.dto.response.RecommendSkillResponse;
import com.example.demo.learning.dto.response.WeekDetailResponse;
import com.example.demo.learning.entity.Learning;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.entity.LearningWeek;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.entity.Portfolio;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.entity.Resume;

@Service
public class LearningService {

    @Autowired
    private LearningDao learningDao;
    @Autowired
    private LearningWeekDao learningWeekDao;
    @Autowired
    private LearningDayDao learningDayDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private ResumeDao resumeDao;
    @Autowired
    private CoverLetterDao coverLetterDao;
    @Autowired
    private PortfolioDao portfolioDao;

    @Autowired
    private CreateRoadmapAgent createRoadmapAgent;
    @Autowired
    private RecommendSkillAgent recommendSkillAgent;
    @Autowired
    private RefineRoadmapAgent refineRoadmapAgent;

    // 학습 로드맵 생성
    public AiCreateRoadmapResponse createLearning(Integer memberId, List<String> purposes, List<String> skills,
            int studyHours) {

        Member member = memberDao.findById(memberId);

        RoadmapRequest request = new RoadmapRequest();
        request.setMemberId(memberId);
        request.setJobRole(member.getJobRole());
        request.setPurposes(purposes);
        request.setSkills(skills);
        request.setStudyHours(studyHours);

        return createRoadmapAgent.generateRoadmap(request);
    }

    // 학습 로드맵 수정
    public AiCreateRoadmapResponse refineRoadmap(RoadmapRefineRequest request) {
        return refineRoadmapAgent.refineRoadmap(request);
    }

    // 학습 로드맵 저장
    @Transactional
    public LearningDetailResponse saveRoadmap(AiCreateRoadmapResponse roadmap) {
        // 1. Learning 저장
        Learning learning = new Learning();
        learning.setMemberId(roadmap.getMemberId());
        learning.setTitle(roadmap.getTitle());
        learning.setStatus("진행중");

        learningDao.insert(learning);
        Integer learningId = learning.getLearningId();

        // 2. Week 저장
        int weekNum = 1;
        for (AiCreateWeekResponse weekData : roadmap.getWeeks()) {
            LearningWeek week = new LearningWeek();
            week.setLearningId(learningId);
            week.setWeekNumber(weekNum++);
            week.setTitle(weekData.getTitle());
            week.setStatus("예정");
            week.setLearningWeekSummary(weekData.getLearningWeekSummary());

            learningWeekDao.insert(week);
            Integer weekId = week.getWeekId();

            // 3. Day 저장
            int dayNum = 1;
            for (AiCreateDayResponse dayData : weekData.getDays()) {
                LearningDay day = new LearningDay();
                day.setWeekId(weekId);
                day.setDayNumber(dayNum++);
                day.setTitle(dayData.getTitle());
                day.setContent(dayData.getContent());
                day.setStatus("예정");

                learningDayDao.insert(day);
            }
        }

        return getLearningById(learningId);
    }

    // 학습 ID로 상세 조회
    public LearningDetailResponse getLearningById(int learningId) {
        // 1. Learning 조회
        Learning learning = learningDao.selectedByLearningId(learningId);

        // 2. DTO 변환
        LearningDetailResponse response = new LearningDetailResponse();
        response.setLearningId(learning.getLearningId());
        response.setTitle(learning.getTitle());
        response.setStatus(learning.getStatus());
        response.setMemberId(learning.getMemberId());

        // 3. DTO 리스트 생성
        List<WeekDetailResponse> weekList = new ArrayList<>();

        List<LearningWeek> weekEntities = learningWeekDao.selectListByLearningId(learningId);

        // 4. 각 Week 엔티티를 DTO로 변환
        for (LearningWeek w : weekEntities) {
            WeekDetailResponse weekDto = new WeekDetailResponse();
            weekDto.setWeekId(w.getWeekId());
            weekDto.setWeekNumber(w.getWeekNumber());
            weekDto.setTitle(w.getTitle());
            weekDto.setStatus(w.getStatus());
            weekDto.setWeekSummary(w.getLearningWeekSummary());

            // 5. 현재 Week에 포함된 Day 목록을 DTO로 담기 위한 리스트 준비
            List<DayDetailResponse> dayList = new ArrayList<>();

            // 6. 현재 WeekId에 속한 Day 엔티티들 조회
            List<LearningDay> dayEntities = learningDayDao.selectListByWeekId(w.getWeekId());

            // 7. Day 엔티티 하나씩 DTO로 변환
            for (LearningDay d : dayEntities) {
                DayDetailResponse dayDto = new DayDetailResponse();
                dayDto.setDayId(d.getDayId());
                dayDto.setDayNumber(d.getDayNumber());
                dayDto.setTitle(d.getTitle());
                dayDto.setContent(d.getContent());
                dayDto.setStatus(d.getStatus());
                dayDto.setLearningDaySummary(d.getLearningDaySummary());
                // 리스트에 추가
                dayList.add(dayDto);
            }

            // 8. Week DTO 안에 Day 리스트 주입
            weekDto.setDays(dayList);

            // 9. 최종 Week 리스트에 weekDto 추가
            weekList.add(weekDto);
        }

        // 10. 최상위 DTO에 Week 리스트 삽입
        response.setWeeks(weekList);

        return response;
    }

    // 사용자가 가진 전체 학습 리스트 조회
    public List<Learning> getLearningListByMember(int memberId) {
        return learningDao.selectListByMemberId(memberId);
    }

    // 상태(학습중/완료 등)로 리스트 조회
    public List<LearningResponse> getLearningListByStatus(int memberId, String status) {
        // 1. DAO에서 Entity 리스트 조회
        List<Learning> learningList = learningDao.selectListByStatus(memberId, status);

        // 2. 변환된 DTO를 담을 리스트 생성
        List<LearningResponse> dtoList = new ArrayList<>();
        // 3. 각 Entity를 LearningResponse DTO로 변환
        for (Learning learning : learningList) {

            // DTO 생성
            LearningResponse dto = new LearningResponse();
            dto.setLearningId(learning.getLearningId());
            dto.setTitle(learning.getTitle());
            dto.setStatus(learning.getStatus());

            // 리스트에 추가
            dtoList.add(dto);
        }

        // 4. DTO 리스트 반환
        return dtoList;
    }

    // 로드맵 생성중 부족한 역량 추천
    public RecommendSkillResponse recommendSkillsFromFeedback(int memberId) {

        // 1. 각 종합 피드백 추출
        List<Resume> resumes = resumeDao.selectResumesByMemberId(memberId);
        List<String> resumeFeedbackRaw = new ArrayList<>();
        for (Resume r : resumes) {
            resumeFeedbackRaw.add(r.getResumeFeedback()); // JSON 그대로
        }

        List<CoverLetter> coverLetters = coverLetterDao.selectAllCoverLetters(memberId);
        List<String> coverFeedbackRaw = new ArrayList<>();
        for (CoverLetter cl : coverLetters) {
            coverFeedbackRaw.add(cl.getCoverFeedback());
        }

        List<Portfolio> portfolios = portfolioDao.selectPortfoliosByMemberId(memberId);
        List<String> portfolioFeedbackRaw = new ArrayList<>();
        for (Portfolio p : portfolios) {
            portfolioFeedbackRaw.add(p.getPortfolioFeedback());
        }

        // 2. Service에서 Feedback Merge 처리
        StringBuilder sb = new StringBuilder();

        sb.append("=== Resume Feedback ===\n");
        for (String r : resumeFeedbackRaw) {
            sb.append(r).append("\n\n");
        }

        sb.append("=== Cover Letter Feedback ===\n");
        for (String c : coverFeedbackRaw) {
            sb.append(c).append("\n\n");
        }

        sb.append("=== Portfolio Feedback ===\n");
        for (String p : portfolioFeedbackRaw) {
            sb.append(p).append("\n\n");
        }

        String mergedFeedback = sb.toString();

        return recommendSkillAgent.recommendSkillFromFeedback(mergedFeedback);
    }

    // 사용자별 학습 개수 조회
    public Map<String, Integer> getLearningCount(int memberId) {
        int ongoing = learningDao.countByMemberId(memberId, "진행중");
        int completed = learningDao.countByMemberId(memberId, "완료");

        Map<String, Integer> result = new HashMap<>();
        result.put("ongoing", ongoing);
        result.put("completed", completed);

        return result;
    }

    // 학습 상태 또는 기타 정보 업데이트
    public int updateLearning(Learning learning) {
        return learningDao.update(learning);
    }
}
