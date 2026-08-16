package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.PortfolioImage;

@Mapper
public interface PortfolioImageDao {

  public int insertPortfolioImage(PortfolioImage portfolioImage); //포트폴리오 이미지 단일 저장

  public List<PortfolioImage> selectImagesByPortfolioId(Integer portfolioId); // 특정 포트폴리오의 모든 이미지 조회

  public PortfolioImage selectBeforePage(@Param("portfolioId") Integer portfolioId, @Param("pageNo") Integer pageNo); // 이전 페이지 피드백 조회

  public PortfolioImage selectImageByPortfolioIdAndPageNo(
      @Param("portfolioId") Integer portfolioId,
      @Param("pageNo") Integer pageNo); // 특정 포트폴리오의 특정 페이지 이미지 조회


  // 통계 및 유틸리티
  public int countImagesByPortfolioId(Integer portfolioId); // 특정 포트폴리오의 총 이미지 수 조회

}