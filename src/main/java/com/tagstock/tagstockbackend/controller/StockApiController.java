package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.dto.StockAiResponseDto;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}