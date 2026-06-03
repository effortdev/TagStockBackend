package com.tagstock.tagstockbackend.config;

import com.tagstock.tagstockbackend.security.JwtAuthenticationFilter;
import com.tagstock.tagstockbackend.security.JwtTokenProvider;
import com.tagstock.tagstockbackend.security.oauth2.CustomOAuth2UserService;
import com.tagstock.tagstockbackend.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    // 🌟 추가된 부분: OAuth2 처리를 위한 서비스와 핸들러 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT를 사용할 때는 서버가 세션(기억)을 유지하지 않도록 STATELESS로 설정해야 합니다!
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 🌟 수정된 부분: 회원가입, 로그인 외에 OAuth2 인증 경로("/oauth2/**")도 누구나 접근 가능하게 허용
                        .requestMatchers("/api/v1/members/signup", "/api/v1/members/login", "/oauth2/**", "/api/v1/batch/test/**").permitAll()
                        // 나머지 모든 요청은 반드시 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )
                // 🌟 새로 추가된 부분: OAuth2 로그인 설정 연결
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                // 방금 만든 JwtAuthenticationFilter를 스프링의 기본 인증 필터 앞에 끼워 넣습니다.
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}