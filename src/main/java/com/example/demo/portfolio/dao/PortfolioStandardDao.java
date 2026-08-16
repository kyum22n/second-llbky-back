package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.portfolio.entity.PortfolioStandard;

/*
-- 1. 포트폴리오 평가 표준 테이블 (템플릿)
CREATE TABLE portfolio_standard (
    standard_id SERIAL PRIMARY KEY,
    standard_name VARCHAR(100) NOT NULL,       
    standard_description TEXT,
    prompt_template TEXT NOT NULL,
    weight_percentage INTEGER DEFAULT 20,
    job_group VARCHAR(100),                      
    job_role VARCHAR(100),                       
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    evaluation_items TEXT,
    score_range_description TEXT
);

-
*/


@Mapper
public interface PortfolioStandardDao {

    // standardId(기준 ID)로 하나의 평가 기준을 조회하는 메소드
    public PortfolioStandard selectStandardById(int standardId);

    // 등록된 모든 평가 기준을 조회하는 메소드
    public List<PortfolioStandard> selectAllStandards();

    // 직군 + 직무로 평가 기준을 조회하는 메소드 (일반 기준 포함)
    public List<PortfolioStandard> selectStandardsByJobInfo(String jobGroup, String jobRole);

}
