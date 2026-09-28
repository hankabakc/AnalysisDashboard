package com.sistek.sos.analysis_dashboard;

import com.sistek.sos.analysis_dashboard.config.ProductionMetrics;
import com.sistek.sos.analysis_dashboard.services.LineService;
import com.sistek.sos.analysis_dashboard.services.PeriodService;
import com.sistek.sos.analysis_dashboard.services.PlcService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /actuator/prometheus: BT'nin izleme sistemi Basic auth ile kazır (ADMIN/APIUSER). Dev kullanıcıları testte yüklüdür.
 * Sabit saat 2025-07-31 12:43 (yerel) → "bu vardiya" 1. vardiya; Hat 1'de 197 barkod, hepsi ERROR (bağlantı hatası).
 * Spring Boot testlerde metrik dışa aktarımını kapatır; @AutoConfigureObservability açar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@Import(TestcontainersConfig.class)
class PrometheusMetricsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProductionMetrics metrics;

    @Autowired
    private LineService lineService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private Clock clock;

    private String scrape() throws Exception {
        return mvc.perform(get("/actuator/prometheus").with(httpBasic("apiuser", "apiuser123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("APIUSER Basic ile 200: toplayıcı sinyal yaşı, takılı barkod, hat durumu ve bu vardiyanın sayıları (panoyla aynı)")
    void exposesIntegrationHealthMetrics() throws Exception {
        metrics.refresh();
        String body = scrape();

        // Örnek verideki son sinyal 09:42:56.86Z, sabit saat 09:43:00Z → 3 sn
        assertTrue(body.contains("dashboard_collector_signal_age_seconds 3.0"), body);
        assertTrue(body.contains("dashboard_barcodes_stuck 0.0"), body);
        assertTrue(body.contains("dashboard_line_running{line=\"1\"} 1.0"), body);
        assertTrue(body.contains("dashboard_line_running{line=\"3\"} 0.0"), body);
        assertTrue(body.contains("dashboard_shift_barcodes{line=\"1\",status=\"error\"} 197.0"), body);
        assertTrue(body.contains("dashboard_shift_barcodes{line=\"1\",status=\"sent\"} 0.0"), body);
        assertTrue(body.contains("dashboard_shift_barcodes{line=\"1\",status=\"pending\"} 0.0"), body);
        // İç sistem bilgisi etiketlere girmez
        assertTrue(!body.contains("192.168.1.181"), "PLC adresi metrikte");
    }

    @Test
    @DisplayName("Kimliksiz 401, USER 403, yanlış parola 401; ADMIN 200")
    void onlyMonitoringRolesCanScrape() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic("user", "user123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/prometheus").with(httpBasic("apiuser", "yanlis-parola")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Başarılı kazıma denetim kaydı üretmez (15 sn'de bir LOGIN_SUCCESS yığılmaz); başarısız deneme LOGIN_FAILURE olarak kalır")
    void scrapingDoesNotFloodTheAuditLog() throws Exception {
        long lastId = jdbc.queryForObject("SELECT coalesce(max(id), 0) FROM app_audit_log", Long.class);
        scrape();
        scrape();
        scrape();
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app_audit_log WHERE id > ?", Long.class, lastId));

        mvc.perform(get("/actuator/prometheus").with(httpBasic("apiuser", "yanlis-parola")));
        assertEquals("LOGIN_FAILURE apiuser", jdbc.queryForObject(
                "SELECT event || ' ' || actor FROM app_audit_log WHERE id > ?", String.class, lastId));
    }

    @Test
    @DisplayName("Tazeleme hata verirse değerler kalkar: eski sayı güncelmiş gibi durmaz")
    void failedRefreshClearsValues() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PlcService failing = mock(PlcService.class);
        ProductionMetrics isolated = new ProductionMetrics(registry, failing, lineService, periodService, clock);
        when(failing.findFirst()).thenThrow(new IllegalStateException("veritabanı yok"));

        isolated.refresh();

        assertTrue(Double.isNaN(registry.get("dashboard.collector.signal.age").gauge().value()));
        assertTrue(Double.isNaN(registry.get("dashboard.barcodes.stuck").gauge().value()));
        assertTrue(registry.find("dashboard.line.running").gauges().isEmpty());
        assertTrue(registry.find("dashboard.shift.barcodes").gauges().isEmpty());
    }

    @Test
    @DisplayName("Açık actuator uçları yalnızca health ve prometheus: env hâlâ kapalı")
    void otherActuatorEndpointsStayClosed() throws Exception {
        mvc.perform(get("/actuator/env").with(httpBasic("admin", "admin123")))
                .andExpect(content().string(not(containsString("spring.datasource"))));
    }
}
