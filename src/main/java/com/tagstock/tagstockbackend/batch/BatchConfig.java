package com.tagstock.tagstockbackend.batch;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import com.tagstock.tagstockbackend.domain.StockAiAnalysis;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import com.tagstock.tagstockbackend.repository.StockAiAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DailyStockPriceRepository priceRepository;
    private final StockAiAnalysisRepository aiAnalysisRepository;

    // 한 번에 처리할 데이터 건수 (메모리 최적화를 위해 100건씩 끊어서 처리)
    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job aiAnalysisJob() {
        return new JobBuilder("aiAnalysisJob", jobRepository)
                .start(aiAnalysisStep())
                .build();
    }

    @Bean
    public Step aiAnalysisStep() {
        // Spring Boot 3.x부터는 StepBuilder에 jobRepository와 transactionManager가 필수입니다.
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .<DailyStockPrice, StockAiAnalysis>chunk(CHUNK_SIZE, transactionManager)
                .reader(stockPriceReader())
                .processor(aiPatternProcessor())
                .writer(aiAnalysisWriter())
                .build();
    }

    // 1. Reader: DB에서 주식 시세 데이터를 CHUNK_SIZE 만큼씩 읽어옵니다.
    @Bean
    public RepositoryItemReader<DailyStockPrice> stockPriceReader() {
        return new RepositoryItemReaderBuilder<DailyStockPrice>()
                .name("stockPriceReader")
                .repository(priceRepository)
                .methodName("findAll")
                .pageSize(CHUNK_SIZE)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    // 2. Processor: 읽어온 시세 데이터를 기반으로 AI 분석(가짜 데이터)을 수행합니다.
    @Bean
    public ItemProcessor<DailyStockPrice, StockAiAnalysis> aiPatternProcessor() {
        return item -> {
            log.info(">>>> [Batch Processor] AI 분석 중: {} ({})", item.getStockName(), item.getStockCode());

            // 실제로는 여기서 Ollama(로컬 AI) API를 호출하지만, 현재는 시뮬레이션 데이터를 반환합니다.
            String mockPattern = "바닥권에서 거래량이 급증하며 상승 반전을 준비하는 패턴";
            String mockTags = "[\"#골든크로스임박\", \"#외인매집중\"]";

            // 주가에 따라 태그 분기 처리 (간단한 로직)
            if (item.getClosePrice() != null && item.getClosePrice() > 100000) {
                mockTags = "[\"#실적턴어라운드\", \"#박스권돌파\"]";
            }

            return StockAiAnalysis.builder()
                    .stockCode(item.getStockCode())
                    .aiPattern(mockPattern)
                    .aiTags(mockTags)
                    .updatedAt(LocalDateTime.now())
                    .build();
        };
    }

    // 3. Writer: 가공된 AI 분석 데이터를 DB에 저장합니다.
    @Bean
    public ItemWriter<StockAiAnalysis> aiAnalysisWriter() {
        return items -> {
            aiAnalysisRepository.saveAll(items);
            log.info("==== [Batch Writer] {}개의 종목 AI 데이터 DB 저장 완료 ====", items.size());
        };
    }
}