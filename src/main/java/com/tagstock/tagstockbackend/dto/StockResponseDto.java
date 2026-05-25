package com.tagstock.tagstockbackend.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

public class StockResponseDto {

    @Getter
    @Builder
    public static class Preview {
        private String code;
        private String name;
        private String price;
        private String rate;
        private List<String> tags;
    }
}