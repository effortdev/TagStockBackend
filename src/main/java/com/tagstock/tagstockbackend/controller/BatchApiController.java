package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.service.KisApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchApiController {

    private final JobLauncher jobLauncher;
    private final Job aiAnalysisJob;
    // 🌟 @Autowired 대신 Lombok의 @RequiredArgsConstructor를 활용해 final로 깔끔하게 주입
    private final KisApiService kisApiService;
    // 🌟 Spring Batch의 DB 기록을 뒤져오는 핵심 탐색기 주입
    private final JobExplorer jobExplorer;

    // 🌟 2. 진짜 배치 상태를 조회하는 API 추가
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 'aiAnalysisJob' 이라는 이름으로 실행된 가장 최근 기록 1개를 찾습니다.
            JobInstance lastJobInstance = jobExplorer.getLastJobInstance("aiAnalysisJob");

            if (lastJobInstance == null) {
                response.put("status", "NONE");
                return ResponseEntity.ok(response);
            }

            JobExecution jobExecution = jobExplorer.getLastJobExecution(lastJobInstance);

            // 현재 상태 (STARTED, COMPLETED, FAILED 등)
            response.put("status", jobExecution.getStatus().toString());

            int readCount = 0;
            int writeCount = 0;
            int skipCount = 0;

            // Step별로 읽은 건수, 쓴 건수, 에러(Skip)난 건수를 합산합니다.
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                readCount += stepExecution.getReadCount();
                writeCount += stepExecution.getWriteCount();
                skipCount += stepExecution.getSkipCount();
            }

            response.put("readCount", readCount);
            response.put("writeCount", writeCount);
            response.put("skipCount", skipCount);

        } catch (Exception e) {
            log.error("배치 상태 조회 중 에러 발생", e);
            response.put("status", "ERROR");
        }

        return ResponseEntity.ok(response);
    }

    // 🌟 주소 충돌 방지: /api/v1/batch/test/kis 로 접속되도록 수정
    @GetMapping("/test/kis")
    public String testKisApi() {
        String[] targetStocks = {"005930", "000660", "035420"}; // 삼성, 하이닉스, 네이버

        for (String code : targetStocks) {
            kisApiService.updateStockPrice(code);
            try {
                // 🌟 한국투자증권 API 차단 방어용 0.3초 휴식 (필수!)
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.error("대기 중 에러", e);
            }
        }
        return "3개 종목 수집 완료! DB를 확인하세요.";
    }

    @PostMapping("/start")
    public ResponseEntity<String> startBatch() {
        try {
            log.info("관리자 요청: AI 분석 배치(Job) 수동 기동 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(aiAnalysisJob, jobParameters);

            return ResponseEntity.ok("배치가 성공적으로 백그라운드에서 실행되었습니다.");
        } catch (Exception e) {
            log.error("배치 실행 실패", e);
            return ResponseEntity.internalServerError().body("배치 실행 실패: " + e.getMessage());
        }
    }
}