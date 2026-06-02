package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.dto.StockAiResponseDto;
import com.tagstock.tagstockbackend.dto.StockChartDataDto;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockApiController {

    private final DailyStockPriceRepository priceRepository;
    // (참고: StockService는 추후 복잡한 비즈니스 로직이 추가될 때 다시 주입해서 사용하면 됩니다!)

    // 🌟 프론트엔드의 요청 주소(/api/v1/stocks)와 정확히 일치하는 단일 엔드포인트
    @GetMapping
    public ResponseEntity<List<StockAiResponseDto>> getStocksByTag(
            @RequestParam(name = "tag", defaultValue = "#골든크로스임박") String tag) {

        // 🌟 Repository에서 방금 만든 조인(Join) 쿼리를 호출하여 태그 필터링 결과를 바로 프론트엔드로 전달합니다.
        List<StockAiResponseDto> response = priceRepository.findStocksByAiTag(tag);

        return ResponseEntity.ok(response);
    }

    // StockApiController.java 내부에 기존 @GetMapping 아래에 추가
    @GetMapping("/{stockCode}")
    public ResponseEntity<StockAiResponseDto> getStockDetail(@PathVariable(name = "stockCode") String stockCode) {
        return priceRepository.findStockDetailByCode(stockCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{stockCode}/chart")
    public ResponseEntity<List<StockChartDataDto>> getStockChartData(@PathVariable(name = "stockCode") String stockCode) {
        // 1. 최신 7일 치 데이터를 가져옵니다.
        List<DailyStockPrice> recentPrices = priceRepository.findTop7ByStockCodeOrderByBaseDateDesc(stockCode);

        // 2. 차트는 과거 -> 현재(왼쪽 -> 오른쪽) 순서로 그려져야 하므로,
        // 데이터를 DTO로 변환한 뒤 리스트의 순서를 뒤집어줍니다(Reverse).
        List<StockChartDataDto> chartData = recentPrices.stream()
                .map(d -> new StockChartDataDto(
                        d.getBaseDate().format(DateTimeFormatter.ofPattern("MM/dd")), // 날짜를 '05/17' 형태로 변환
                        d.getClosePrice()
                ))
                .collect(Collectors.toList());

        Collections.reverse(chartData);

        return ResponseEntity.ok(chartData);
    }
}