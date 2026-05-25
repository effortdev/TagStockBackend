package com.tagstock.tagstockbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "stock_ai_analysis")
public class StockAiAnalysis {

    @Id
    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "ai_pattern", length = 500)
    private String aiPattern;

    // JSON 배열 형태의 문자열로 저장 (예: '["#골든크로스임박", "#외인매집중"]')
    @Column(name = "ai_tags", columnDefinition = "TEXT")
    private String aiTags;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 배치가 돌면서 데이터를 업데이트할 때 사용하는 편의 메서드
    public void updateAnalysis(String aiPattern, String aiTags) {
        this.aiPattern = aiPattern;
        this.aiTags = aiTags;
        this.updatedAt = LocalDateTime.now();
    }
}