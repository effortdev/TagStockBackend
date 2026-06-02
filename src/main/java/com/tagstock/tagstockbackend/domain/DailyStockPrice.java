package com.tagstock.tagstockbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "daily_stock_price",
        uniqueConstraints = {
                // 🌟 같은 종목코드와 날짜의 중복 데이터 저장을 원천 차단
                @UniqueConstraint(name = "uk_stock_date", columnNames = {"stock_code", "base_date"})
        },
        indexes = {
                @Index(name = "idx_stock_date", columnList = "stock_code, base_date")
        }
)
public class DailyStockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "close_price")
    private Long closePrice; // 🌟 Integer보다 큰 범위의 금융값 대응

    @Column(name = "volume")
    private Long volume;

    // 🌟 데이터 수집 시점 기록
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}