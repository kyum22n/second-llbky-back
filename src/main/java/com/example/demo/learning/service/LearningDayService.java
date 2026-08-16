package com.example.demo.learning.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.learning.MemoCheckAgent;
import com.example.demo.ai.learning.RewriteMemoAgent;
import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.dao.LearningWeekDao;
import com.example.demo.learning.dto.response.MemoCheckResponse;
import com.example.demo.learning.entity.Learning;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.entity.LearningWeek;

@Service
public class LearningDayService {

    // DAO
    @Autowired
    private LearningDayDao learningDayDao;
    @Autowired
    private LearningWeekDao learningWeekDao;
    @Autowired
    private LearningDao learningDao;

    // AI Agent
    @Autowired
    private MemoCheckAgent memoCheckAgent;
    @Autowired
    private RewriteMemoAgent rewriteMemoAgent;

    // 일일 학습 생성
    public int createDay(LearningDay learningDay) {
        return learningDayDao.insert(learningDay);
    }

    // dayId로 상세 조회
    public LearningDay getDayById(int dayId) {
        return learningDayDao.selectedByDayId(dayId);
    }

    // 특정 주차의 전체 일일 학습 리스트 조회
    public List<LearningDay> getDayListByWeekId(int weekId) {
        return learningDayDao.selectListByWeekId(weekId);
    }

    // 특정 학습 플랜의 전체 일일 학습 목록 조회
    public List<LearningDay> getDayListByLearningId(int learningId) {
        return learningDayDao.selectListByLearningId(learningId);
    }

    // 주차 ID + 일차 번호로 조회
    public LearningDay getDayByWeekIdAndDayNumber(int weekId, int dayNumber) {
        return learningDayDao.selectByWeekIdAndDayNumber(weekId, dayNumber);
    }

    // 일일 학습 메모 저장
    public LearningDay submitMemo(int dayId, String learningDaySummary) {

        // 1. 일일 학습 정보 조회
        LearningDay day = learningDayDao.selectedByDayId(dayId);
        if (day == null) {
            throw new RuntimeException("Day not found");
        }

        // 2. AI 검증
        MemoCheckResponse checkResult = memoCheckAgent.execute(day, learningDaySummary);

        // 3. AI 메모 정리
        LearningDay updatedDay = rewriteMemoAgent.execute(day, learningDaySummary, checkResult);

        // 4. 주차 상태 업데이트
        updateWeekStatus(updatedDay.getWeekId());

        return updatedDay;
    }

    // 일일 학습 결과를 주차 진행률에 반영
    private void updateWeekStatus(int weekId) {

        // 1. 해당 주차의 모든 Day 가져오기
        List<LearningDay> days = learningDayDao.selectListByWeekId(weekId);

        // 2. 상태 계산
        boolean allComplete = days.stream().allMatch(d -> "완료".equals(d.getStatus()));
        boolean anyStarted = days.stream().anyMatch(d -> "완료".equals(d.getStatus()) || "진행 중".equals(d.getStatus()));

        // 3. 기존 Week 불러오기
        LearningWeek week = learningWeekDao.selectedByWeekId(weekId);

        // 4. 상태 업데이트
        if (allComplete) {
            week.setStatus("완료");
        } else if (anyStarted) {
            week.setStatus("진행 중");
        } else {
            week.setStatus("예정");
        }

        // 5. DB 업데이트
        learningWeekDao.update(week);

        // -----------------------------
        // Learning 전체 완료 체크
        // -----------------------------

        int learningId = week.getLearningId();
        List<LearningWeek> weekList = learningWeekDao.selectListByLearningId(learningId);

        boolean allWeeksComplete = weekList.stream()
                .allMatch(w -> "완료".equals(w.getStatus()));

        if (allWeeksComplete) {
            // learning 상태 완료로 변경
            Learning learning = new Learning();
            learning.setLearningId(learningId);
            learning.setStatus("완료");

            learningDao.update(learning);
        }
    }

    // 일차 업데이트
    public int updateDay(LearningDay learningDay) {
        return learningDayDao.update(learningDay);
    }
}
