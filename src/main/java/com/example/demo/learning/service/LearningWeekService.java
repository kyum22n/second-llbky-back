package com.example.demo.learning.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.learning.dao.LearningWeekDao;
import com.example.demo.learning.entity.LearningWeek;

@Service
public class LearningWeekService {

    @Autowired
    private LearningWeekDao learningWeekDao;

    // 주차 생성
    public int createWeek(LearningWeek learningWeek) {
        return learningWeekDao.insert(learningWeek);
    }

    // weekId로 주차 상세 조회
    public LearningWeek getWeekById(int weekId) {
        return learningWeekDao.selectedByWeekId(weekId);
    }

    // learningId에 속한 전체 주차 리스트
    public List<LearningWeek> getWeekListByLearningId(int learningId) {
        return learningWeekDao.selectListByLearningId(learningId);
    }

    // 학습 ID + 주차 번호로 조회
    public LearningWeek getWeekByLearningIdAndWeekNumber(int learningId, int weekNumber) {
        return learningWeekDao.selectByLearningIdAndWeekNumber(learningId, weekNumber);
    }

    // 주차 업데이트
    public int updateWeek(LearningWeek learningWeek) {
        return learningWeekDao.update(learningWeek);
    }
}
