package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.dto.MemberRequestDto;
import com.tagstock.tagstockbackend.dto.MemberResponseDto;
import com.tagstock.tagstockbackend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody MemberRequestDto.SignUp signUpDto) {
        return ResponseEntity.ok(memberService.signUp(signUpDto));
    }

    @PostMapping("/login")
    public ResponseEntity<MemberResponseDto> login(@RequestBody MemberRequestDto.Login loginDto) {
        return ResponseEntity.ok(memberService.login(loginDto));
    }
}