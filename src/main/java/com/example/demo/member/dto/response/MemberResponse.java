package com.example.demo.member.dto.response;

import lombok.Data;

@Data
public class MemberResponse {
    private Integer memberId;
    private String memberName;
    private String loginId;
    private String memberPassword;
    private String memberEmail;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
}
