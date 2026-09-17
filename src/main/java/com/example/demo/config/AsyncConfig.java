package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 포트폴리오 페이지별 분석처럼 오래 걸리는 순차 LLM 호출을
 * HTTP 요청 스레드와 분리해서 백그라운드로 처리하기 위한 설정.
 * LLM rate limit 때문에 병렬로 늘려도 의미가 없어 스레드 수는 소규모로 유지한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "portfolioAnalysisExecutor")
    public Executor portfolioAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("portfolio-analysis-");
        executor.initialize();
        return executor;
    }
}
