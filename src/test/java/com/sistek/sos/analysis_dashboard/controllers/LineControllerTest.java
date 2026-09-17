package com.sistek.sos.analysis_dashboard.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Hat Detay Sayfası Filtreleme, Sıralama ve Sayfalama Testleri (T-007).
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "USER")
class LineControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /line/1: İlk sayfada tam 50 satır, Sayfa 1 / 27 ve toplam 1319 gösterir")
    void hat1IlkSayfa50SatirVeSayfalamaBasar() throws Exception {
        mvc.perform(get("/line/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hat 1")))
                .andExpect(content().string(containsString("1319 kayıttan 1-50 arası")))
                .andExpect(content().string(containsString("Sayfa 1 / 27")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(50))
                // Tarih ham ISO biçiminde (2025-07-08T11:27:51) değil, gg.aa.yyyy SS:dd:ss basılır
                .andExpect(xpath("//table/tbody/tr[1]/td[2]").string(matchesPattern("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}")));
    }

    @Test
    @DisplayName("GET /line/999: Olmayan hat 404 döner (API ile tutarlı)")
    void olmayanHat404Doner() throws Exception {
        mvc.perform(get("/line/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /line/1?page=26: Son sayfada tam 19 satır gösterir (1319 - 26*50)")
    void hat1SonSayfada19SatirBasar() throws Exception {
        mvc.perform(get("/line/1").param("page", "26"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319 kayıttan 1301-1319 arası")))
                .andExpect(content().string(containsString("Sayfa 27 / 27")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(19));
    }

    @Test
    @DisplayName("GET /line/1?barcodeQuery=GU132: Barkod araması ile toplam 172 kayıt bulur")
    void hat1BarkodAramasi172KayitBulur() throws Exception {
        mvc.perform(get("/line/1").param("barcodeQuery", "GU132"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("172 kayıttan 1-50 arası")))
                .andExpect(content().string(containsString("Sayfa 1 / 4")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(50));
    }

    @Test
    @DisplayName("GET /line/5?barcodeQuery=GU128: Hat 5'te arama ile toplam 2 kayıt bulur")
    void hat5BarkodAramasi2KayitBulur() throws Exception {
        mvc.perform(get("/line/5").param("barcodeQuery", "GU128"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 kayıttan 1-2 arası")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(2));
    }

    @Test
    @DisplayName("GET /line/1?status=ERROR: 1319 kayıt; ?status=SENT: 0 kayıt ve boş mesaj basar")
    void durumFiltresiCalisir() throws Exception {
        // ERROR filtresi
        mvc.perform(get("/line/1").param("status", "ERROR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319 kayıttan 1-50 arası")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(50));

        // SENT filtresi (veritabanında SENT yok -> 0 kayıt ve boş durum mesajı)
        mvc.perform(get("/line/1").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 kayıt bulundu")))
                .andExpect(content().string(containsString("Filtreye uygun kayıt bulunamadı.")))
                .andExpect(xpath("//table").doesNotExist());
    }

    @Test
    @DisplayName("GET /line/1?sort=asc ile ?sort=desc: Farklı ilk barkodu basar")
    void siralamaYonuDegisir() throws Exception {
        MvcResult resDesc = mvc.perform(get("/line/1").param("sort", "desc"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult resAsc = mvc.perform(get("/line/1").param("sort", "asc"))
                .andExpect(status().isOk())
                .andReturn();

        String htmlDesc = resDesc.getResponse().getContentAsString();
        String htmlAsc = resAsc.getResponse().getContentAsString();

        // İki farklı sıralamanın ilk barkod satırları farklı olmalıdır
        assertNotEquals(htmlDesc, htmlAsc);
    }

    @Test
    @DisplayName("GET /line/1?page=-5&sort=xyz&status=HACK: Çökmez, varsayılanlarla 50 satır basar")
    void gecersizParametrelerVarsayilanaDoner() throws Exception {
        mvc.perform(get("/line/1")
                        .param("page", "-5")
                        .param("sort", "xyz")
                        .param("status", "HACK"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319 kayıttan 1-50 arası")))
                .andExpect(content().string(containsString("Sayfa 1 / 27")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(50));
    }

    @Test
    @DisplayName("GET /line/1?barcodeQuery=GU132&page=1: Sayfalama bağlantılarında barcodeQuery=GU132 parametresi korunur")
    void sayfalamaBaglantilariParametreleriKorur() throws Exception {
        mvc.perform(get("/line/1")
                        .param("barcodeQuery", "GU132")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(xpath("//a[contains(text(), 'Önceki')]/@href").string(containsString("barcodeQuery=GU132")))
                .andExpect(xpath("//a[contains(text(), 'Sonraki')]/@href").string(containsString("barcodeQuery=GU132")));
    }
}
