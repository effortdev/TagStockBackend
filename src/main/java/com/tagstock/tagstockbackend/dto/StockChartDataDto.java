package com.tagstock.tagstockbackend.dto;

// 프론트엔드의 Recharts가 요구하는 { date: '05/17', price: 78000 } 형태에 딱 맞춘 DTO입니다.
public record StockChartDataDto(
        String date,
        Long price
) {}