package com.example.demo.newstrend.dto.response;

import lombok.Data;

// 감정 비율

@Data
public class SentimentRatio {
    private double positive;  // 비율 %
    private double neutral;
    private double negative;
}
