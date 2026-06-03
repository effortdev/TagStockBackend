package com.tagstock.tagstockbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // 🌟 스프링 컨테이너에 RestTemplate을 Bean으로 등록해 주는 마법의 코드
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}