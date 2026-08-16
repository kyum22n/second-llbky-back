package com.example.demo.portfolio.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class GuideContentData {
    private List<GuideStepData> steps = new ArrayList<>();
    private String lastUpdated;
    
    // private String version;
}
