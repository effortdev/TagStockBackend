package com.tagstock.tagstockbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "daily_stock_price",
        indexes = {
                // 종목코드와 날짜로 조회하는 경우가 많으므로 복합 인덱스 생성
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
    private Integer closePrice;

    @Column(name = "volume")
    private Long volume;
}