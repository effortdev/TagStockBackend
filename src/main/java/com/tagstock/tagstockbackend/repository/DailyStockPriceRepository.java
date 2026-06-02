package com.tagstock.tagstockbackend.repository;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tagstock.tagstockbackend.dto.StockAiResponseDto;
import org.springframework.data.jpa.repository.Query;

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

    // 두 테이블을 Join하여 프론트엔드에서 필요한 데이터만 DTO로 바로 쏙 뽑아옵니다.
    @Query("SELECT new com.tagstock.tagstockbackend.dto.StockAiResponseDto(d.stockCode, d.stockName, d.closePrice, a.aiPattern, a.aiTags) " +
            "FROM DailyStockPrice d JOIN StockAiAnalysis a ON d.stockCode = a.stockCode " +
            "ORDER BY d.closePrice DESC")
    List<StockAiResponseDto> findStocksWithAiData();

    // 🌟 프론트에서 넘어온 태그(예: "#골든크로스임박")가 포함된 종목만 필터링해서 가져옵니다.
    @Query("SELECT new com.tagstock.tagstockbackend.dto.StockAiResponseDto(d.stockCode, d.stockName, d.closePrice, a.aiPattern, a.aiTags) " +
            "FROM DailyStockPrice d JOIN StockAiAnalysis a ON d.stockCode = a.stockCode " +
            "WHERE a.aiTags LIKE %:tag% " +
            "ORDER BY d.closePrice DESC")
    List<StockAiResponseDto> findStocksByAiTag(@Param("tag") String tag);
}