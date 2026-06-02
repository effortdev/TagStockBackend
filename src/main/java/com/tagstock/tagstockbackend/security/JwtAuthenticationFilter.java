package com.tagstock.tagstockbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 클라이언트의 요청(Request) 헤더에서 토큰을 빼옵니다.
        String token = resolveToken(request);

        // 2. 토큰이 존재하고 유효한지 검사합니다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰이 정상이라면, 유저 정보를 꺼내서 스프링 시큐리티의 '인증된 사용자 명부(SecurityContext)'에 이름을 올려줍니다.
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 4. 다음 필터로 요청을 넘깁니다. (토큰이 없거나 가짜면 명부에 이름이 없는 상태로 넘어갑니다)
        filterChain.doFilter(request, response);
    }

    // 헤더에서 "Bearer [토큰값]" 형태의 문자열을 찾아 "[토큰값]"만 잘라내는 도우미 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}