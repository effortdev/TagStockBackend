package com.tagstock.tagstockbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🌟 1. 실시간 30일 데이터 기반 AI 프롬프트 생성 및 호출
    public Map<String, Object> analyzeStockTrendRealtime(String stockName, String stockCode, String priceTrendStr) {
        log.info(">>>> [실시간 AI 분석] {} ({}) 30일 트렌드 분석 시작...", stockName, stockCode);

        // 프롬프트: '오늘 하루'가 아닌 '최근 30일 흐름'을 주입합니다.
        String prompt = String.format(
                "너는 전문 주식 애널리스트야. %s(종목코드: %s)의 최근 30일간의 종가 흐름은 다음과 같아: [%s]. " +
                        "이 가격 추세(상승, 하락, 횡보, 변동성 등)를 분석해서 반드시 아래의 JSON 형식으로만 출력해. " +
                        "태그(tags)는 반드시 ['#골든크로스임박', '#외인매집중', '#과매도구간', '#박스권돌파', '#실적턴어라운드', '#신고가경신', '#바닥다지기'] 중 2개를 골라.\n" +
                        "{\"summary\": \"50자 이내 핵심 한 줄 평\", \"details\": \"최근 30일 가격 흐름에 대한 정밀 분석\", \"risk\": \"투자 시 주의할 리스크\", \"tags\": [\"#태그1\", \"#태그2\"]}",
                stockName, stockCode, priceTrendStr
        );

        String aiResponse = callLocalOllamaApi(prompt);
        return parseAiResponse(aiResponse);
    }

    // 🌟 2. 로컬 Ollama API 호출 (기존 Batch에 있던 로직과 동일)
    private String callLocalOllamaApi(String prompt) {
        String ollamaUrl = "http://localhost:11434/api/generate";
        Map<String, Object> request = new HashMap<>();
        request.put("model", "gemma2:2b"); // 사용 중인 모델명
        request.put("prompt", prompt);
        request.put("stream", false);

        try {
            Map<String, Object> response = restTemplate.postForObject(ollamaUrl, request, Map.class);
            return (String) response.get("response");
        } catch (Exception e) {
            log.error("Ollama API 호출 실패", e);
            return "{}"; // 실패 시 빈 JSON 반환
        }
    }

    // 🌟 3. JSON 안전 파싱 로직
    private Map<String, Object> parseAiResponse(String aiResponse) {
        Map<String, Object> result = new HashMap<>();
        String cleanedResponse = aiResponse.replace("```json", "").replace("```", "").trim();

        try {
            JsonNode rootNode = objectMapper.readTree(cleanedResponse);
            result.put("summary", rootNode.path("summary").asText("분석 요약 불가"));
            result.put("details", rootNode.path("details").asText("상세 분석을 생성하지 못했습니다."));
            result.put("risk", rootNode.path("risk").asText("리스크 데이터 확인 불가"));
            result.put("tags", rootNode.has("tags") ? rootNode.get("tags") : objectMapper.createArrayNode());
        } catch (Exception e) {
            log.error("AI JSON 파싱 에러: {}", cleanedResponse);
            result.put("error", true);
        }
        return result;
    }
}