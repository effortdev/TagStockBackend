package com.tagstock.tagstockbackend.security.oauth2;

import com.tagstock.tagstockbackend.domain.Member;
import com.tagstock.tagstockbackend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 구글에서 넘어온 유저 정보 빼오기
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        // DB에 이메일이 있는지 확인하고, 없으면 새로 저장(회원가입)
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .email(email)
                            // 소셜 로그인은 비밀번호를 안 쓰지만 DB 제약조건 때문에 임의의 값 저장
                            .password(UUID.randomUUID().toString())
                            .name(name)
                            .role("ROLE_USER")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return memberRepository.save(newMember);
                });

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole())),
                attributes,
                "email" // 구글 정보 중 'email'을 고유 식별자로 사용
        );
    }
}