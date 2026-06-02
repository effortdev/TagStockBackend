package com.tagstock.tagstockbackend.service;

import com.tagstock.tagstockbackend.domain.Member;
import com.tagstock.tagstockbackend.dto.MemberRequestDto;
import com.tagstock.tagstockbackend.dto.MemberResponseDto;
import com.tagstock.tagstockbackend.repository.MemberRepository;
import com.tagstock.tagstockbackend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public String signUp(MemberRequestDto.SignUp signUpDto) {
        if (memberRepository.existsByEmail(signUpDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        Member member = Member.builder()
                .email(signUpDto.getEmail())
                .password(passwordEncoder.encode(signUpDto.getPassword()))
                .name(signUpDto.getName())
                .role("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);
        return "회원가입이 성공적으로 완료되었습니다.";
    }

    @Transactional
    public MemberResponseDto login(MemberRequestDto.Login loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword());

        // 여기서 CustomUserDetailsService의 loadUserByUsername이 호출되어 검증됩니다.
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        String token = jwtTokenProvider.generateToken(authentication);

        Member member = memberRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        return MemberResponseDto.builder()
                .email(member.getEmail())
                .name(member.getName())
                .tokenType("Bearer")
                .accessToken(token)
                .build();
    }
}