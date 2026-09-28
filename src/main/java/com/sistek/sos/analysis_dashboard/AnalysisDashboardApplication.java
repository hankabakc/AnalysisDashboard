package com.sistek.sos.analysis_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling // metrik tazeleme (ProductionMetrics) ve dev'de sahte toplayıcı
public class AnalysisDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisDashboardApplication.class, args);
    }

    /** Sağlık kontrolünün "şimdi"si. Veritabanı saatleri UTC olduğu için UTC; testler sabit bir saatle değiştirir. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
