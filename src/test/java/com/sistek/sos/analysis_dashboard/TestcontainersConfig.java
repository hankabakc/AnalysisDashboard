package com.sistek.sos.analysis_dashboard;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testler için Docker'da geçici PostgreSQL. Bağlantı bilgisi @ServiceConnection ile otomatik verilir.
 * Şemayı ve örnek veriyi Flyway kurar (bkz. src/test/resources/config/application.properties).
 * Aynı yapılandırmayı kullanan test sınıfları Spring context'ini, dolayısıyla tek konteyneri paylaşır.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }
}
