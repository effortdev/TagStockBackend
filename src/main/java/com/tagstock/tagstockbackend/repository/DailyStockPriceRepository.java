package com.tagstock.tagstockbackend.repository;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStockPriceRepository extends JpaRepository<DailyStockPrice, Long> {

    // 1. 특정 종목의 특정 날짜 데이터 단건 조회
    Optional<DailyStockPrice> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate);

    // 🌟 새로 추가할 부분: 특정 종목의 가장 최근(날짜 내림차순) 데이터 1건을 가져옵니다.
    Optional<DailyStockPrice> findTopByStockCodeOrderByBaseDateDesc(String stockCode);

    // 2. 특정 종목의 최근 한 달(또는 지정 기간) 주가 흐름을 날짜 오름차순으로 조회 (차트 그리기용)
    List<DailyStockPrice> findByStockCodeAndBaseDateBetweenOrderByBaseDateAsc(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate
    );
}