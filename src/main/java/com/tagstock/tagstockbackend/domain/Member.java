package com.tagstock.tagstockbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 아이디 (이메일)
    @Column(nullable = false, unique = true, length = 50)
    private String email;

    // 암호화된 비밀번호
    @Column(nullable = false, length = 100)
    private String password;

    // 사용자 이름 또는 닉네임
    @Column(nullable = false, length = 20)
    private String name;

    // 권한 (예: ROLE_USER, ROLE_ADMIN)
    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}