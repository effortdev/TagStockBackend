package com.tagstock.tagstockbackend.service;

import com.tagstock.tagstockbackend.domain.DailyStockPrice;
import com.tagstock.tagstockbackend.repository.DailyStockPriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
     * 3. 주식 일자별 시세 조회 API (프론트엔드 차트 데이터용)
     */
    public List<Map<String, Object>> getDailyChartPrice(String stockCode) {
        if (accessToken == null) issueAccessToken();

        // 주식 일자별 시세 URL (최근 30영업일 치를 반환합니다)
        String url = domain + "/uapi/domestic-stock/v1/quotations/inquire-daily-price" +
                "?FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + stockCode +
                "&FID_PERIOD_DIV_CODE=D" + // D: 일 단위
                "&FID_ORG_ADJ_PRC=0";      // 0: 수정주가 미적용

        // FHKST01010400 : 주식현재가 일자별 조회용 TR_ID
        HttpEntity<String> entity = new HttpEntity<>(createHeaders("FHKST01010400"));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // output 필드에 날짜별 데이터가 배열(List) 형태로 들어있습니다.
                List<Map<String, Object>> outputList = (List<Map<String, Object>>) response.getBody().get("output");
                log.info(">>>> [한국투자증권] {} 차트용 과거 데이터 {}건 로드 성공!", stockCode, outputList.size());
                return outputList;
            }
        } catch (Exception e) {
            log.error("[ERROR] 한국투자증권 일별 시세 조회 실패: ", e);
        }
        return null;
    }

    @Autowired
    private DailyStockPriceRepository priceRepository;

    public void updateStockPrice(String stockCode) {
        Map<String, Object> data = getCurrentPrice(stockCode);
        if (data != null) {
            // 🌟 Builder 패턴을 사용하여 객체 생성과 동시에 값을 주입
            DailyStockPrice stock = DailyStockPrice.builder()
                    .stockCode(stockCode)
                    .stockName("삼성전자")
                    .closePrice(Long.parseLong(data.get("stck_prpr").toString())) // Long 타입으로 맞춤
                    .volume(Long.parseLong(data.get("acml_vol").toString()))
                    .baseDate(java.time.LocalDate.now())
                    .build();

            priceRepository.save(stock);
            log.info(">>>> [DB 저장 완료] {} 데이터 적재 완료", stockCode);
        }
    }
}