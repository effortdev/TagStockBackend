package com.tagstock.tagstockbackend.service;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KisApiService {

    @Value("${kis.api.domain}")
    private String domain;

    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    // 접속 토큰을 임시로 저장해둘 변수
    private String accessToken;

    // HTTP 통신을 위한 RestTemplate
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private DailyStockPriceRepository priceRepository;

    /**
     * 1. 한국투자증권 접속 토큰(Access Token) 발급
     */
    public void issueAccessToken() {
        String url = domain + "/oauth2/tokenP";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credentials");
        requestBody.put("appkey", appKey);
        requestBody.put("appsecret", appSecret);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                this.accessToken = (String) response.getBody().get("access_token");
                log.info(">>>> [한국투자증권] Access Token 발급 성공!");
            }
        } catch (Exception e) {
            log.error("[ERROR] 한국투자증권 토큰 발급 실패: ", e);
        }
    }

    /**
     * 공통 HTTP Header 생성 도우미 메서드
     * @param trId 한국투자증권 API 종류를 구분하는 고유 ID
     */
    private HttpHeaders createHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        return headers;
    }

    /**
     * 2. 주식 현재가 조회 API (종가, 거래량 등 수집용)
     */
    public Map<String, Object> getCurrentPrice(String stockCode) {
        if (accessToken == null) issueAccessToken(); // 토큰이 없으면 1회 발급

        // 주식현재가 시세 URL 및 쿼리 파라미터 (J: 주식/ETF)
        String url = domain + "/uapi/domestic-stock/v1/quotations/inquire-price" +
                "?FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + stockCode;

        // FHKST01010100 : 주식현재가 시세 조회용 TR_ID
        HttpEntity<String> entity = new HttpEntity<>(createHeaders("FHKST01010100"));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> output = (Map<String, Object>) response.getBody().get("output");
                // stck_prpr(현재가/종가), acml_vol(누적거래량)
                log.info(">>>> [한국투자증권] {} 현재가: {}원, 누적거래량: {}주",
                        stockCode, output.get("stck_prpr"), output.get("acml_vol"));
                return output;
            }
        } catch (Exception e) {
            log.error("[ERROR] 한국투자증권 현재가 조회 실패: ", e);
        }
        return null;
    }

    /**
     * 3. [핵심] 최근 30일(영업일 기준) 주가 데이터를 가져오는 메서드
     * (프론트엔드 차트 및 AI 실시간 분석용 통합)
     */
    public List<Map<String, Object>> getRecent30DaysPrice(String stockCode) {
        log.info(">>>> [한국투자증권] {} 30일 치 과거 데이터 조회 요청...", stockCode);
        if (accessToken == null) issueAccessToken();

        // KIS 기간별 시세 API 주소 (파라미터: J=주식, D=일별, 0=수정주가 반영)
        String url = domain + "/uapi/domestic-stock/v1/quotations/inquire-daily-price" +
                "?FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + stockCode +
                "&FID_PERIOD_DIV_CODE=D" +
                "&FID_ORG_ADJ_PRC=0";

        // FHKST01010400 : 주식 일자별 시세 조회용 TR_ID
        HttpEntity<String> entity = new HttpEntity<>(createHeaders("FHKST01010400"));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful() && body != null && body.containsKey("output")) {
                List<Map<String, Object>> outputList = (List<Map<String, Object>>) body.get("output");
                log.info(">>>> [한국투자증권] {} 차트/AI용 과거 데이터 {}건 로드 성공!", stockCode, outputList.size());
                return outputList;
            }
        } catch (Exception e) {
            log.error("[ERROR] 한국투자증권 30일 시세 API 호출 중 에러 발생", e);
        }
        return Collections.emptyList();
    }

    /**
     * 4. DB에 주가 정보를 저장하는 메서드
     */
    public void updateStockPrice(String stockCode) {
        Map<String, Object> data = getCurrentPrice(stockCode);

        if (data != null) {
            String realStockName = switch(stockCode) {
                case "000660" -> "SK하이닉스";
                case "035420" -> "NAVER";
                case "005380" -> "현대차";
                case "035720" -> "카카오";
                case "000270" -> "기아";
                case "005930" -> "삼성전자";
                default -> "알수없음";
            };

            DailyStockPrice stock = DailyStockPrice.builder()
                    .stockCode(stockCode)
                    .stockName(realStockName)
                    .baseDate(LocalDate.now())
                    .closePrice(Long.parseLong(data.get("stck_prpr").toString()))
                    .volume(Long.parseLong(data.get("acml_vol").toString()))
                    .build();

            priceRepository.save(stock);
            log.info(">>>> [DB 저장 완료] {} ({}) 데이터 적재 완료", realStockName, stockCode);
        }
    }

    /**
     * 5. [핵심] 30일 치 데이터를 가져와서 '중복 없이' DB에 갱신(Sync)하는 메서드
     */
    public void sync30DaysDataToDbSafe(String stockCode) {
        log.info(">>>> [데이터 동기화] {} 종목의 30일 치 데이터 중복 검사 및 DB 적재 시작...", stockCode);

        // 1. KIS API에서 30일 치 데이터를 가져옵니다.
        List<Map<String, Object>> recentPrices = getRecent30DaysPrice(stockCode);

        // 종목 이름 매핑 (기존과 동일)
        String realStockName = switch(stockCode) {
            case "000660" -> "SK하이닉스";
            case "035420" -> "NAVER";
            case "005380" -> "현대차";
            case "035720" -> "카카오";
            case "000270" -> "기아";
            case "005930" -> "삼성전자";
            default -> "알수없음";
        };

        int insertCount = 0;

        // 2. 30개의 데이터를 하나씩 까보면서 중복 검사를 합니다.
        for (Map<String, Object> data : recentPrices) {
            String dateStr = data.get("stck_bsop_date").toString(); // "20260603"
            LocalDate baseDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 🌟 [핵심] DB에 이 종목의 해당 날짜 데이터가 있는지 팩트 체크!
            boolean isExist = priceRepository.existsByStockCodeAndBaseDate(stockCode, baseDate);

            // 3. 데이터가 없을 때만 DB에 새롭게 저장합니다.
            if (!isExist) {
                DailyStockPrice stock = DailyStockPrice.builder()
                        .stockCode(stockCode)
                        .stockName(realStockName)
                        .baseDate(baseDate) // KIS API가 준 진짜 과거 날짜!
                        .closePrice(Long.parseLong(data.get("stck_clpr").toString()))
                        .volume(Long.parseLong(data.get("acml_vol").toString()))
                        .build();

                priceRepository.save(stock);
                insertCount++;
            }
        }

        log.info(">>>> [데이터 동기화 완료] {} - 총 30건 중 신규 저장: {}건 (나머지는 이미 존재하여 스킵)", realStockName, insertCount);
    }
}