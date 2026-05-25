package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.dto.StockResponseDto;
import com.tagstock.tagstockbackend.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockApiController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<StockResponseDto.Preview>> getStocksByTag(
            @RequestParam(name = "tag", defaultValue = "#골든크로스임박") String tag) {

        List<StockResponseDto.Preview> response = stockService.getStocksByTag(tag);
        return ResponseEntity.ok(response);
    }
}