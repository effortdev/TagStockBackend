package com.tagstock.tagstockbackend.security.oauth2;

import com.tagstock.tagstockbackend.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. 유저 인증 정보를 바탕으로 JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(authentication);

        // 2. 프론트엔드(React)의 특정 주소로 리다이렉트 하면서 주소창 뒤에 토큰을 달아줍니다.
        // 주의: 현재 React가 실행 중인 포트(보통 Vite는 5173)를 정확히 적어주어야 합니다!
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}