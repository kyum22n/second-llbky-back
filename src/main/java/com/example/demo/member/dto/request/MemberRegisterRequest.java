package com.example.demo.member.dto.request;

import lombok.Data;

@Data
public class MemberRegisterRequest {
    private String name;
    private String loginId;
    private String password;
    private String email;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
}
