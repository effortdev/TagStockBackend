package com.tagstock.tagstockbackend.dto;

public record StockAiResponseDto(
        String stockCode,
        String stockName,
        Integer closePrice,
        String aiPattern,
        String aiTags
) {}