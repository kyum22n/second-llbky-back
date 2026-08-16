package com.example.demo.resume.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.resume.FeedbackResumeAgent;
import com.example.demo.ai.resume.ResumeCoachAgent;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.request.ResumeCoachRequest;
import com.example.demo.resume.dto.response.ResumeCoachResponse;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResumeService {
  @Autowired
  private ResumeDao resumeDao;
  @Autowired
  private FeedbackResumeAgent resumeAiAgent;
  @Autowired
  private ResumeCoachAgent resumeCoachAgent;
  @Autowired
  private ObjectMapper mapper;

  // 이력서 작성
  @Transactional
  public int createResume(Resume resume) throws Exception {
    // 이력서 저장
    resumeDao.insertResume(resume);

    // pk 가져오기
    int newId = resume.getResumeId();

    // AI 자동 분석 실행
    resumeAiAgent.analyze(resume.getMemberId(), newId);

    return resume.getResumeId();
  }

  // 이력서 상세 조회
  public Resume getResume(int resumeId) {
    return resumeDao.selectResumeById(resumeId);
  }

  // 이력서 목록 조회
  public List<Resume> getResumeList(int memberId) {
    return resumeDao.selectResumesByMemberId(memberId);
  }

  // 이력서 수정
  public int updateResume(Resume resume) {
    // 1) 기존 데이터 조회
    Resume original = resumeDao.selectResumeById(resume.getResumeId());
    if (original == null) {
      throw new RuntimeException("이력서를 찾을 수 없습니다.");
    }

    // 2) 변경된 필드만 반영
    // 제목
    if (resume.getTitle() != null) {
      original.setTitle(resume.getTitle());
    }
    // careerInfo
    if (resume.getCareerInfo() != null) {
      original.setCareerInfo(resume.getCareerInfo());
    }
    // educationInfo
    if (resume.getEducationInfo() != null) {
      original.setEducationInfo(resume.getEducationInfo());
    }
    // skills
    if (resume.getSkills() != null) {
      original.setSkills(resume.getSkills());
    }
    // activities
    if (resume.getActivities() != null) {
      original.setActivities(resume.getActivities());
    }
    // certificates
    if (resume.getCertificates() != null) {
      original.setCertificates(resume.getCertificates());
    }
    // 3) 병합된 객체로 업데이트
    return resumeDao.updateResume(original);
  }

  // 이력서 삭제
  public int deleteResume(int resumeId) {
    return resumeDao.deleteResume(resumeId);
  }

  // AI 분석
  public ResumeReportResponse analyzeResume(int memberId, int resumeId) throws Exception {
    ResumeReportResponse aiResult = resumeAiAgent.analyze(memberId, resumeId);
    log.info("AI 분석 결과: {}", aiResult);
    return aiResult;
  }

  // AI 분석 조회
  public ResumeReportResponse getResumeReport(int resumeId) throws Exception {
    Resume resume = resumeDao.selectResumeById(resumeId);

    if (resume == null) {
      throw new RuntimeException("이력서를 찾을 수 없습니다.");
    }
    return mapper.readValue(resume.getResumeFeedback(), ResumeReportResponse.class);
  }

  // 실시간 코칭
  public ResumeCoachResponse coachResponse(ResumeCoachRequest request) throws Exception {
    return resumeCoachAgent.coach(request);
  }

}
