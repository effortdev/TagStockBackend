package com.tagstock.tagstockbackend.config;

import com.tagstock.tagstockbackend.security.JwtAuthenticationFilter;
import com.tagstock.tagstockbackend.security.JwtTokenProvider;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 🌟 JWT를 사용할 때는 서버가 세션(기억)을 유지하지 않도록 STATELESS로 설정해야 합니다!
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 회원가입과 로그인은 토큰이 없는 상태에서 접근해야 하므로 모두 허용(permitAll)
                        .requestMatchers("/api/v1/members/signup", "/api/v1/members/login").permitAll()
                        // 나머지 모든 요청(배치, 주식 조회 등)은 반드시 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )
                // 우리가 방금 만든 JwtAuthenticationFilter를 스프링의 기본 인증 필터 앞에 끼워 넣습니다.
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}