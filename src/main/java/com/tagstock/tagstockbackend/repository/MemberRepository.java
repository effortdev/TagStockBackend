package com.tagstock.tagstockbackend.repository;

import com.tagstock.tagstockbackend.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 로그인 시 이메일로 회원 정보를 찾기 위한 메서드
    Optional<Member> findByEmail(String email);

    // 회원가입 시 이메일 중복 체크를 위한 메서드
    boolean existsByEmail(String email);
}