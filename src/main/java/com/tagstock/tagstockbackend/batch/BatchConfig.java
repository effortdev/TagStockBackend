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
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DailyStockPriceRepository priceRepository;
    private final StockAiAnalysisRepository aiAnalysisRepository;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job aiAnalysisJob() {
        return new JobBuilder("aiAnalysisJob", jobRepository)
                .start(aiAnalysisStep())
                .build();
    }

    @Bean
    public Step aiAnalysisStep() {
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .<DailyStockPrice, StockAiAnalysis>chunk(CHUNK_SIZE, transactionManager)
                .reader(stockPriceReader())
                .processor(aiPatternProcessor())
                .writer(aiAnalysisWriter())
                .build();
    }

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

    // 🌟 2. Processor: Ollama API 연동 로직으로 전면 교체
    @Bean
    public ItemProcessor<DailyStockPrice, StockAiAnalysis> aiPatternProcessor() {
        return item -> {
            log.info(">>>> [Batch Processor] {} ({}) AI 분석 요청 중...", item.getStockName(), item.getStockCode());

            // 1. AI에게 던질 프롬프트(질문) 작성
            String prompt = String.format(
                    "너는 전문 주식 애널리스트야. %s(종목코드: %s)의 오늘 종가가 %d원이고, 거래량은 %d주야. 이 정보를 바탕으로 현재 주식의 패턴과 향후 전망을 딱 한 줄(50자 이내)로 분석해 줘. 그리고 이 상황에 어울리는 주식 트렌드 해시태그 2개를 만들어 줘. (예시: #반등시도 #거래량급증)",
                    item.getStockName(), item.getStockCode(), item.getClosePrice(), item.getVolume()
            );

            // 2. Ollama API 호출
            String aiResponse = callLocalOllamaApi(prompt);

            // 임시로 전체 응답을 패턴에 저장하고, 태그는 고정값으로 둡니다.
            // (나중에 AI의 응답 문자열에서 정규식으로 태그만 예쁘게 파싱하는 로직을 추가하면 완벽해집니다)
            return StockAiAnalysis.builder()
                    .stockCode(item.getStockCode())
                    .aiPattern(aiResponse)
                    .aiTags("[\"#AI분석완료\"]")
                    .updatedAt(LocalDateTime.now())
                    .build();
        };
    }

    @Bean
    public ItemWriter<StockAiAnalysis> aiAnalysisWriter() {
        return items -> {
            aiAnalysisRepository.saveAll(items);
            log.info("==== [Batch Writer] {}개의 종목 AI 데이터 DB 저장 완료 ====", items.size());
        };
    }

    // 🌟 3. Ollama와 통신하는 헬퍼 메서드
    private String callLocalOllamaApi(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:11434/api/generate"; // Ollama 기본 포트

        // Ollama API 요청 규격에 맞게 JSON 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        // ⚠️ 현재 PC에 설치된 Ollama 모델명(예: eeve, eeve-bllossom, llama3 등)으로 반드시 변경해 주세요!
        requestBody.put("model", "gemma2:2b");
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false); // 응답을 한 번에 받기 위해 false 설정

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("response"); // AI의 실제 답변 추출
            }
        } catch (Exception e) {
            log.error("[ERROR] Ollama API 호출 실패: ", e);
            return "AI 분석 서버에 연결할 수 없습니다.";
        }
        return "AI 분석 결과를 가져오지 못했습니다.";
    }
}