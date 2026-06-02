package com.tagstock.tagstockbackend.service;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import com.tagstock.tagstockbackend.domain.StockAiAnalysis;
import com.tagstock.tagstockbackend.dto.StockResponseDto;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import com.tagstock.tagstockbackend.repository.StockAiAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockAiAnalysisRepository aiAnalysisRepository;
    private final DailyStockPriceRepository priceRepository;

    public List<StockResponseDto.Preview> getStocksByTag(String tag) {
        // 1. 해당 태그를 가진 AI 분석 데이터를 가져옵니다.
        List<StockAiAnalysis> analyses = aiAnalysisRepository.findByAiTagsContaining(tag);

        return analyses.stream().map(analysis -> {
            // 2. 태그 문자열 정리 및 공백 제거
            String cleanTags = analysis.getAiTags().replace("[", "").replace("]", "").replace("\"", "");
            List<String> tagList = Arrays.stream(cleanTags.split(","))
                    .map(String::trim) // " #외인매집중" 같은 공백 제거
                    .collect(Collectors.toList());

            // 3. 종목 코드로 실제 주가 테이블(DailyStockPrice)에서 오늘 날짜의 주식 정보를 찾습니다.
            DailyStockPrice stockInfo = priceRepository.findTopByStockCodeOrderByBaseDateDesc(
                    analysis.getStockCode()
            ).orElse(null);

            // 4. 조회된 데이터가 있으면 실제 값을, 없으면 기본값을 넣습니다.
            String realName = (stockInfo != null) ? stockInfo.getStockName() : "알 수 없는 종목";
            String realPrice = (stockInfo != null) ? String.format("%,d", stockInfo.getClosePrice()) : "0";

            return StockResponseDto.Preview.builder()
                    .code(analysis.getStockCode())
                    .name(realName)
                    .price(realPrice)
                    .rate("+0.00%") // 등락률 계산 로직은 나중에 고도화할 때 추가
                    .tags(tagList)
                    .build();
        }).collect(Collectors.toList());
    }
}