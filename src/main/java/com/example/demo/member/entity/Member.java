package com.example.demo.member.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Member {
    private Integer memberId;
    private String memberName;
    private String loginId;
    private String memberPassword;
    private String memberEmail;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
