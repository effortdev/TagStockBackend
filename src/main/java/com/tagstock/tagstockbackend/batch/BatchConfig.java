package com.tagstock.tagstockbackend.batch;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import com.tagstock.tagstockbackend.domain.StockAiAnalysis;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import com.tagstock.tagstockbackend.repository.StockAiAnalysisRepository;
import com.tagstock.tagstockbackend.service.KisApiService;
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
import org.springframework.batch.repeat.RepeatStatus;
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
    private final KisApiService kisApiService;

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

    @Bean
    public ItemProcessor<DailyStockPrice, StockAiAnalysis> aiPatternProcessor() {
        return item -> {
            log.info(">>>> [Batch Processor] {} ({}) AI 분석 요청 중...", item.getStockName(), item.getStockCode());

            // 🌟 1. JSON 포맷을 강제하는 초정밀 프롬프트 작성
            String prompt = String.format(
                    "너는 전문 주식 애널리스트야. %s(종목코드: %s)의 오늘 종가는 %d원이고, 거래량은 %d주야. " +
                            "이 정보를 바탕으로 분석 결과를 반드시 아래의 JSON 형식으로만 출력해. 마크다운이나 다른 설명은 절대 덧붙이지 마.\n" +
                            "{\"summary\": \"50자 이내 핵심 한 줄 평\", \"details\": \"주가와 거래량을 바탕으로 한 상세 분석\", \"risk\": \"투자 시 주의할 리스크나 하락 요인\"}",
                    item.getStockName(), item.getStockCode(), item.getClosePrice(), item.getVolume()
            );

            String aiResponse = callLocalOllamaApi(prompt);

            // 🌟 2. AI가 간혹 마크다운(```json)을 붙여서 대답할 경우를 대비한 문자열 정제(클렌징)
            String cleanedResponse = aiResponse.replace("```json", "").replace("```", "").trim();

            String dynamicTags;
            if (item.getClosePrice() >= 150000) {
                dynamicTags = "[\"#실적턴어라운드\", \"#박스권돌파\"]";
            } else if (item.getVolume() != null && item.getVolume() >= 1000000) {
                dynamicTags = "[\"#외인매집중\", \"#과매도구간\"]";
            } else {
                dynamicTags = "[\"#골든크로스임박\"]";
            }

            return StockAiAnalysis.builder()
                    .stockCode(item.getStockCode())
                    .aiPattern(cleanedResponse) // 이제 단순 문장이 아닌 {"summary":"...", "details":"...", ...} 형태가 저장됩니다!
                    .aiTags(dynamicTags)
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

    // BatchConfig.java에 추가할 수집 단계 (Reader/Processor/Writer 구조)
    @Bean
    public Step apiDataCollectionStep() {
        return new StepBuilder("apiDataCollectionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 여기서 kisApiService를 호출하여 삼성전자 데이터를 긁어와 저장
                    kisApiService.updateStockPrice("005930");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}