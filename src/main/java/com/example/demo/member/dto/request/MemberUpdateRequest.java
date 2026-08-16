package com.example.demo.member.dto.request;

import lombok.Data;

@Data
public class MemberUpdateRequest {
    private String email;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
}
