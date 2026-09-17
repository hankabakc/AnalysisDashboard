package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Genel Pano Şablon Render ve Entegrasyon Testleri.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@WithMockUser(roles = "USER")
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /dashboard: 192.168.1.181, ACTIVE ve 5 hattın adetlerini hat kartı bazında kesin doğrular")
    void dashboardShowsPlcAndLineCounts() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("192.168.1.181")))
                .andExpect(content().string(containsString("ACTIVE")))
                // XPath ile her hat kartı içindeki toplam adedin birebir eşleşmesi ve bağlantısı doğrulanır
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='1']]//div[contains(@class, 'display-6')]").string("1319"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='2']]//div[contains(@class, 'display-6')]").string("6"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='3']]//div[contains(@class, 'display-6')]").string("14"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='4']]//div[contains(@class, 'display-6')]").string("2325"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='5']]//div[contains(@class, 'display-6')]").string("2"))
                // Hat kartlarının /line/{id} bağlantıları
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='1']]/@href").string("/line/1"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='5']]/@href").string("/line/5"))
                // Dış CDN kullanılmaz; varlıklar WebJar olarak pakete gömülü
                .andExpect(content().string(not(containsString("cdn.jsdelivr.net"))))
                .andExpect(content().string(containsString("/webjars/bootstrap/")));
    }
}
