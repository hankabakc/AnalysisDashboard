package com.sistek.sos.analysis_dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 dokümantasyon yapılandırması.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Analysis Dashboard API")
                        .description("Salt okunur üretim verisi panosu REST API dokümantasyonu")
                        .version("v1.0"));
    }
}
