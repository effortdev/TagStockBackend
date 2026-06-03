package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import com.tagstock.tagstockbackend.dto.StockAiResponseDto;
import com.tagstock.tagstockbackend.dto.StockChartDataDto;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import com.tagstock.tagstockbackend.service.KisApiService;
import com.tagstock.tagstockbackend.service.StockAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockApiController {

    private final DailyStockPriceRepository priceRepository;
    private final StockAiService stockAiService;
    private final KisApiService kisApiService;

    // 메인 화면: 태그별 종목 목록 조회
    @GetMapping
    public ResponseEntity<List<StockAiResponseDto>> getStocksByTag(
            @RequestParam(name = "tag", defaultValue = "#골든크로스임박") String tag) {
        List<StockAiResponseDto> response = priceRepository.findStocksByAiTag(tag);
        return ResponseEntity.ok(response);
    }

    // 상세 화면: 종목 기본 정보 조회
    @GetMapping("/{stockCode}")
    public ResponseEntity<DailyStockPrice> getStockDetail(@PathVariable(name = "stockCode") String stockCode) {
        // 🌟 무작정 검색하지 않고, '가장 최신 날짜 1개'만 꺼내오도록 변경!
        return priceRepository.findFirstByStockCodeOrderByBaseDateDesc(stockCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🌟 1. 데이터 수동 갱신 API (버튼 클릭 시 KIS API 1회 조심스럽게 호출 후 DB에 중복 없이 저장)
    @PostMapping("/{stockCode}/sync")
    public ResponseEntity<?> syncStockData(@PathVariable(name = "stockCode") String stockCode) {
        log.info(">>>> 프론트엔드 동기화 요청 수신: {}", stockCode);

        // KIS API에서 가져와서 DB에 저장! (Rate Limit 방어 핵심)
        kisApiService.sync30DaysDataToDbSafe(stockCode);

        return ResponseEntity.ok(Map.of("message", "데이터 갱신이 완료되었습니다."));
    }

    // 🌟 2. 차트 데이터 가져오기 (KIS API 실시간 연동 -> 안전한 우리 DB 조회로 변경)
    @GetMapping("/{stockCode}/chart")
    public ResponseEntity<List<StockChartDataDto>> getStockChartData(@PathVariable(name = "stockCode") String stockCode) {
        // 외부 API가 아닌 우리 DB에서 30일치 데이터를 꺼내옵니다.
        List<DailyStockPrice> dbPrices = priceRepository.findTop30ByStockCodeOrderByBaseDateDesc(stockCode);

        // 프론트엔드 차트용 DTO로 변환
        List<StockChartDataDto> chartData = dbPrices.stream()
                .map(d -> new StockChartDataDto(
                        d.getBaseDate().format(DateTimeFormatter.ofPattern("MM/dd")),
                        d.getClosePrice()
                ))
                .collect(Collectors.toList());

        // 과거 -> 현재 순서로 차트를 그리기 위해 리스트 뒤집기
        Collections.reverse(chartData);

        return ResponseEntity.ok(chartData);
    }

    // 🌟 3. 데이터 기반 AI 실시간 분석 (KIS API 안 찌름! 우리 DB에서 가져옴)
    @PostMapping("/{stockCode}/analyze-realtime")
    public ResponseEntity<?> analyzeStockRealtime(
            @PathVariable(name = "stockCode") String stockCode,
            @RequestBody Map<String, String> payload) {

        String stockName = payload.getOrDefault("stockName", "알수없음");
        log.info(">>>> 프론트엔드 실시간 분석 요청: {} ({})", stockName, stockCode);

        // 우리 DB에서 안전하게 30일 데이터를 꺼내옵니다.
        List<DailyStockPrice> dbPrices = priceRepository.findTop30ByStockCodeOrderByBaseDateDesc(stockCode);

        // 만약 DB가 텅 비어있다면, 갱신부터 하라고 프론트엔드에 알려줍니다.
        if (dbPrices.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "DB에 데이터가 없습니다. 먼저 갱신 버튼을 눌러주세요."));
        }

        // AI에게 던져줄 트렌드 문자열 생성
        String real30DaysTrend = dbPrices.stream()
                .map(data -> String.valueOf(data.getClosePrice()))
                .collect(Collectors.joining(", "));

        log.info(">>>> 추출된 실제 30일 가격 흐름: {}", real30DaysTrend);

        // AI 엔진에 진짜 가격 흐름을 전달!
        Map<String, Object> aiAnalysisResult = stockAiService.analyzeStockTrendRealtime(stockName, stockCode, real30DaysTrend);

        return ResponseEntity.ok(aiAnalysisResult);
    }
}