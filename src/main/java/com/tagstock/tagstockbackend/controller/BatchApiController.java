package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.service.KisApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchApiController {

    private final JobLauncher jobLauncher;
    private final Job aiAnalysisJob;
    // 🌟 @Autowired 대신 Lombok의 @RequiredArgsConstructor를 활용해 final로 깔끔하게 주입
    private final KisApiService kisApiService;

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