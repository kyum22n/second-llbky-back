package com.example.demo.newstrend.dto.request;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NewsAnalysisRequest {
    private String title;
    private String content;
    private String sourceUrl;
    private String sourceName;
    private Integer memberId;
    private LocalDateTime publishedAt;
}
