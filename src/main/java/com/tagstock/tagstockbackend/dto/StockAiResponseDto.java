package com.tagstock.tagstockbackend.dto;

public record StockAiResponseDto(
        String stockCode,
        String stockName,
        Long closePrice,
        String aiPattern,
        String aiTags
) {
    // 🌟 이 생성자 하나면 JPA의 타입 추론 문제(int vs Long)가 한 방에 해결됩니다.
    public StockAiResponseDto(String stockCode, String stockName, Number closePrice, String aiPattern, String aiTags) {
        this(stockCode, stockName, closePrice != null ? closePrice.longValue() : null, aiPattern, aiTags);
    }
}