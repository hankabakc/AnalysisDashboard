package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Tüm hatlarda barkod sorgulama sayfası (/barcodes) ve CSV dışa aktarımı.
 * Sayılar örnek veriden: tüm zamanlarda 3666 barkod, Hat 1'de 1319.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@WithMockUser(roles = "USER")
class BarcodeControllerTest {

    private static final String ROWS = "//table[@aria-label='Barkod listesi']/tbody/tr";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("GET /barcodes?range=all: tüm hatlar (3666, /api/barcodes ile aynı), Hat sütunu, menüde etkin sayfa; tazelenmez")
    void listsBarcodesOfAllLines() throws Exception {
        mvc.perform(get("/barcodes").param("range", "all"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("3666 kayıttan 1-50 arası")))
                .andExpect(xpath(ROWS).nodeCount(50))
                .andExpect(xpath("//table[@aria-label='Barkod listesi']/thead//th[.='HAT']").exists())
                .andExpect(xpath("//nav//a[@aria-current='page']").string("Barkod Sorgulama"))
                .andExpect(xpath("//*[@hx-trigger='refresh']").doesNotExist());
    }

    @Test
    @DisplayName("Filtreler: lineId=1 → 1319; status=ERROR + errorCode=ORA-20100 → yalnız o satırlar; bilinmeyen hat → 0 kayıt (hata değil)")
    void filtersAcrossLines() throws Exception {
        mvc.perform(get("/barcodes").param("range", "all").param("lineId", "1"))
                .andExpect(content().string(containsString("1319 kayıttan 1-50 arası")))
                .andExpect(xpath("//select[@name='lineId']/option[@selected]").string("Hat 1"));

        long orderNotFound = jdbc.queryForObject(
                "SELECT count(*) FROM barcode_data WHERE status = 'ERROR' AND api_result LIKE '%ORA-20100%'", Long.class);
        mvc.perform(get("/barcodes").param("range", "all").param("status", "ERROR").param("errorCode", "ORA-20100"))
                .andExpect(content().string(containsString(orderNotFound + " kayıttan 1-50 arası")))
                .andExpect(xpath(ROWS + "[td[5]!='İş emri bulunamadı']").doesNotExist());

        mvc.perform(get("/barcodes").param("range", "all").param("lineId", "999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 kayıt bulundu")));
    }

    @Test
    @DisplayName("Hat hücresi aynı dönemle hat sayfasına bağlanır; sayfalama bağlantıları hat ve filtreleri korur")
    void linksKeepLineAndFilters() throws Exception {
        mvc.perform(get("/barcodes").param("day", "2025-07-28").param("lineId", "4").param("status", "ERROR"))
                .andExpect(xpath(ROWS + "[1]/td[2]/a/@href").string(containsString("/line/4?")))
                .andExpect(xpath(ROWS + "[1]/td[2]/a/@href").string(containsString("day=2025-07-28")))
                .andExpect(xpath("//a[contains(., 'Sonraki')]/@href").string(containsString("/barcodes?lineId=4")))
                .andExpect(xpath("//a[contains(., 'Sonraki')]/@href").string(containsString("status=ERROR")))
                .andExpect(xpath("//a[contains(., 'Sonraki')]/@href").string(containsString("page=1")));
    }

    @Test
    @DisplayName("Arama metni kaçırılır (<script> düz metin); ham api_result ve PLC adresi sayfaya girmez")
    void escapesInputAndHidesInternals() throws Exception {
        mvc.perform(get("/barcodes").param("range", "all").param("barcodeQuery", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)"))))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
        mvc.perform(get("/barcodes").param("range", "all").param("status", "ERROR"))
                .andExpect(content().string(not(containsString("Failed to send"))))
                .andExpect(content().string(not(containsString("ErrorText"))))
                .andExpect(content().string(not(containsString("192.168.1.181"))));
    }

    @Test
    @DisplayName("GET /barcodes.csv?day=2025-07-28: tüm hatlar, sayfadaki toplamla aynı satır sayısı; tüm zamanlar 400")
    void csvExportOfAllLines() throws Exception {
        long dayTotal = jdbc.queryForObject(
                "SELECT count(*) FROM barcode_data WHERE cre_date >= '2025-07-28 05:00' AND cre_date < '2025-07-29 05:00'", Long.class);
        mvc.perform(get("/barcodes").param("day", "2025-07-28"))
                .andExpect(content().string(containsString(dayTotal + " kayıttan")))
                .andExpect(xpath("//a[contains(., 'CSV indir')]/@href").string(containsString("/barcodes.csv?")));

        String csv = mvc.perform(get("/barcodes.csv").param("day", "2025-07-28"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("barkodlar.csv")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertEquals("﻿Barkod;Hat;Okunma zamanı;Durum;Hata sebebi", lines[0]);
        assertEquals(1 + dayTotal, lines.length);
        assertFalse(csv.contains("Failed to send"));

        mvc.perform(get("/barcodes.csv").param("range", "all"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Panodaki hata dağılımı satırları tüm hatların süzülmüş listesine bağlanır (aynı dönemle)")
    void dashboardErrorBreakdownLinksToBarcodeSearch() throws Exception {
        mvc.perform(get("/dashboard").param("day", "2025-07-28"))
                .andExpect(xpath("//section[@aria-label='ERP Hata Dağılımı']//a[.='İş emri bulunamadı']/@href")
                        .string(containsString("/barcodes?status=ERROR&errorCode=ORA-20100")))
                .andExpect(xpath("//section[@aria-label='ERP Hata Dağılımı']//a[.='İş emri bulunamadı']/@href")
                        .string(containsString("day=2025-07-28")));
    }

    @Test
    @DisplayName("Hat sayfası ortak tabloyu Hat sütunu olmadan kullanır")
    void linePageHasNoLineColumn() throws Exception {
        mvc.perform(get("/line/1").param("range", "all"))
                .andExpect(xpath(ROWS).nodeCount(50))
                .andExpect(xpath("//table[@aria-label='Barkod listesi']/thead//th[.='HAT']").doesNotExist())
                .andExpect(xpath("//nav//a[@aria-current='page']").string("Üretim Panosu"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Kimliksiz istek giriş sayfasına yönlenir")
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/barcodes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/barcodes.csv"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "APIUSER")
    @DisplayName("APIUSER web ekranlarına giremez: 403")
    void apiUserIsForbidden() throws Exception {
        mvc.perform(get("/barcodes"))
                .andExpect(status().isForbidden());
    }
}
