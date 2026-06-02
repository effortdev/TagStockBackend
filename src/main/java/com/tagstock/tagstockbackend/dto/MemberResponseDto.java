package com.tagstock.tagstockbackend.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class MemberResponseDto {
    private String email;
    private String name;
    private String tokenType;
    private String accessToken;
}