package com.tagstock.tagstockbackend.controller;

import com.tagstock.tagstockbackend.service.KisApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private KisApiService kisApiService;

    @GetMapping("/api/test/kis")
    public String testKisApi() {
        // 삼성전자(005930) 실제 데이터 조회 테스트
        kisApiService.getCurrentPrice("005930");
        kisApiService.getDailyChartPrice("005930");
        return "콘솔 로그를 확인해보세요!";
    }

    @PostMapping("/start")
    public ResponseEntity<String> startBatch() {
        try {
            log.info("관리자 요청: AI 분석 배치(Job) 수동 기동 시작");

            // 배치는 동일한 파라미터로 두 번 실행될 수 없으므로, 현재 시간을 파라미터로 넣어 매번 새로운 실행으로 인식하게 합니다.
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