package com.example.demo.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.member.dto.request.MemberLoginRequest;
import com.example.demo.member.dto.request.MemberRegisterRequest;
import com.example.demo.member.dto.request.MemberUpdateRequest;
import com.example.demo.member.dto.response.MemberResponse;
import com.example.demo.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

  private final MemberService memberService;

  /**
   * 회원가입
   */
  @PostMapping("/register")
  public ResponseEntity<String> register(@RequestBody MemberRegisterRequest request) {
    memberService.register(request);
    return ResponseEntity.ok("회원가입이 완료되었습니다.");
  }

  /**
   * 로그인
   */
  @PostMapping("/login")
  public ResponseEntity<MemberResponse> login(@RequestBody MemberLoginRequest request) {
    MemberResponse response = memberService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * 회원 정보 수정
   */
  @PutMapping("/update")
  public ResponseEntity<String> updateMember(
      @RequestParam("memberId") Integer memberId,
      @RequestBody MemberUpdateRequest request) {

    memberService.modifyMember(memberId, request);
    return ResponseEntity.ok("회원 정보가 수정되었습니다.");
  }
}
