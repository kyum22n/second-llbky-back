package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class SentimentScores {
    private Integer positive;
    private Integer neutral; 
    private Integer negative;

}
