package com.sistek.sos.analysis_dashboard;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Testler için Docker'da geçici PostgreSQL. Bağlantı bilgisi @ServiceConnection ile otomatik verilir.
 * Şemayı ve örnek veriyi Flyway kurar (bkz. src/test/resources/config/application.properties).
 * Aynı yapılandırmayı kullanan test sınıfları Spring context'ini, dolayısıyla tek konteyneri paylaşır.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /** Örnek verideki son PLC sinyali (2025-07-31T09:42:56.86Z) ile aynı ana sabitlenmiş saat. */
    public static final Instant NOW = Instant.parse("2025-07-31T09:43:00Z");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    /** Sağlık kontrolü "şimdi"yi bu saatten okur; örnek veri eski olsa da toplayıcı canlı görünür. */
    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
