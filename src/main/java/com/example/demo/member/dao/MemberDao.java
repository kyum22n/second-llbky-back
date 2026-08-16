package com.example.demo.member.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.member.entity.Member;

@Mapper
public interface MemberDao {
    public int insert(Member member);
    public Member findById(@Param("memberId") Integer memberId);
    public Member findByLoginId(@Param("loginId") String loginId);
    public int update(Member member);
}
