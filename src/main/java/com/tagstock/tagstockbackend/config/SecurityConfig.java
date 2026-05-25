package com.tagstock.tagstockbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보호 비활성화 (REST API에서는 주로 꺼둡니다)
                .csrf(AbstractHttpConfigurer::disable)
                // 2. 폼 로그인 비활성화 (우리는 나중에 JWT를 쓸 것이기 때문)
                .formLogin(AbstractHttpConfigurer::disable)
                // 3. HTTP Basic 인증 비활성화 (방금 401 에러를 낸 주범)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 4. 모든 요청에 대해 우선은 인증 없이 접근을 허용합니다 (나중에 여기를 수정할 예정)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}