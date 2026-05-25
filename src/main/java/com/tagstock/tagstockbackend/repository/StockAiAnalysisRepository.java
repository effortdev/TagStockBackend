package com.tagstock.tagstockbackend.repository;

import com.tagstock.tagstockbackend.domain.StockAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAiAnalysisRepository extends JpaRepository<StockAiAnalysis, String> {

    // 특정 해시태그(예: "#외인매집중")를 포함하고 있는 종목 리스트 조회
    // AI 태그가 JSON String 형태로 저장되므로 Containing을 사용해 검색합니다.
    List<StockAiAnalysis> findByAiTagsContaining(String tag);
}