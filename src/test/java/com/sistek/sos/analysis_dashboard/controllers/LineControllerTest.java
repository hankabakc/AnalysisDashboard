package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Hat Detay Sayfası Filtreleme, Sıralama ve Sayfalama Testleri.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@WithMockUser(roles = "USER")
class LineControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /line/1: İlk sayfada tam 50 satır, Sayfa 1 / 27 ve toplam 1319 gösterir")
    void firstPageShows50RowsAndPaging() throws Exception {
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
    void unknownLineReturns404() throws Exception {
        mvc.perform(get("/line/999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/fragments/line/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /line/1?barcodeQuery=GU132&status=ERROR: tablo aynı filtre ve sayfayla tazelenir; arama formu tazelenen alanın dışında")
    void linePageRefreshKeepsFilters() throws Exception {
        mvc.perform(get("/line/1").param("barcodeQuery", "GU132").param("status", "error"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string(containsString("/fragments/line/1?")))
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string(containsString("barcodeQuery=GU132")))
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string(containsString("status=ERROR")))
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string(containsString("page=0")))
                .andExpect(xpath("//div[@hx-trigger='refresh']//form").doesNotExist())
                .andExpect(xpath("//script[@src='/js/refresh.js']/@data-refresh-ms").string("5000"));
    }

    @Test
    @DisplayName("GET /fragments/line/1?barcodeQuery=GU132: yalnızca sayı + tablo + sayfalayıcı döner; menü ve arama formu yok")
    void barcodeTableFragmentRendersTableOnly() throws Exception {
        mvc.perform(get("/fragments/line/1").param("barcodeQuery", "GU132"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("172 kayıttan 1-50 arası")))
                .andExpect(content().string(containsString("Son güncelleme:")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(50))
                .andExpect(xpath("//a[contains(text(), 'Sonraki')]/@href").string(containsString("barcodeQuery=GU132")))
                .andExpect(content().string(not(containsString("<nav"))))
                .andExpect(content().string(not(containsString("<form"))));
    }

    @Test
    @DisplayName("Hata sayfası: 404 için Türkçe mesaj ve panoya dönüş bağlantısı basar, Whitelabel sayfası çıkmaz")
    void errorPageRendersTurkishMessage() throws Exception {
        // MockMvc /error yönlendirmesini kendisi yapmadığı için hata isteği elle kurulur
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Sayfa bulunamadı")))
                .andExpect(xpath("//a[@href='/dashboard' and contains(@class, 'btn')]").exists())
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    @DisplayName("GET /line/1?page=26: Son sayfada tam 19 satır gösterir (1319 - 26*50)")
    void lastPageShowsRemaining19Rows() throws Exception {
        mvc.perform(get("/line/1").param("page", "26"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319 kayıttan 1301-1319 arası")))
                .andExpect(content().string(containsString("Sayfa 27 / 27")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(19));
    }

    @Test
    @DisplayName("GET /line/1?barcodeQuery=GU132: Barkod araması ile toplam 172 kayıt bulur")
    void barcodeSearchFinds172OnLine1() throws Exception {
        mvc.perform(get("/line/1").param("barcodeQuery", "GU132"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("172 kayıttan 1-50 arası")))
                .andExpect(content().string(containsString("Sayfa 1 / 4")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(50));
    }

    @Test
    @DisplayName("GET /line/5?barcodeQuery=GU128: Hat 5'te arama ile toplam 2 kayıt bulur")
    void barcodeSearchFinds2OnLine5() throws Exception {
        mvc.perform(get("/line/5").param("barcodeQuery", "GU128"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 kayıttan 1-2 arası")))
                .andExpect(xpath("//table/tbody/tr").nodeCount(2));
    }

    @Test
    @DisplayName("GET /line/1?status=ERROR: 1319 kayıt; ?status=SENT: 0 kayıt ve boş mesaj basar")
    void statusFilterWorks() throws Exception {
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
    @DisplayName("GET /line/1?errorCode=...: CONNECTION 197, ORA-20124 949, ORA-20100 173; geçersiz kod yok sayılır (1319)")
    void errorCodeFilterWorks() throws Exception {
        mvc.perform(get("/line/1").param("errorCode", "CONNECTION"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("197 kayıttan 1-50 arası")));
        mvc.perform(get("/line/1").param("errorCode", "ORA-20124"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("949 kayıttan 1-50 arası")));
        mvc.perform(get("/line/1").param("errorCode", "ora-20100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("173 kayıttan 1-50 arası")));
        mvc.perform(get("/line/1").param("errorCode", "HACK"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319 kayıttan 1-50 arası")));
    }

    @Test
    @DisplayName("GET /line/1?errorCode=CONNECTION: 'Hata sebebi' sütunu etiketi basar, ERROR rozeti kırmızı; ham api_result sayfaya girmez")
    void errorColumnShowsLabelNotRawApiResult() throws Exception {
        mvc.perform(get("/line/1").param("errorCode", "CONNECTION"))
                .andExpect(status().isOk())
                .andExpect(xpath("//table/thead/tr/th[4]").string(containsString("HATA SEBEBİ")))
                .andExpect(xpath("//table/tbody/tr[1]/td[4]").string("ERP'ye ulaşılamadı"))
                .andExpect(xpath("//table/tbody/tr[1]/td[3]/span[contains(@class, 'text-bg-danger')]").string("ERROR"))
                .andExpect(content().string(not(containsString("Failed to send"))))
                .andExpect(content().string(not(containsString("ifsapplications"))))
                .andExpect(content().string(not(containsString("ErrorText"))));
    }

    @Test
    @DisplayName("GET /line/1: hattın hata dağılımı; her satır listeyi status=ERROR ve o hata koduyla süzen bağlantıdır")
    void lineErrorBreakdownLinksToFilteredList() throws Exception {
        String breakdown = "//section[@aria-label='ERP Hata Dağılımı']";
        mvc.perform(get("/line/1"))
                .andExpect(status().isOk())
                .andExpect(xpath(breakdown + "//li").nodeCount(3))
                .andExpect(xpath(breakdown + "//li[contains(., 'Barkod formatı hatalı')]/span[contains(@class, 'badge')]").string("949"))
                .andExpect(xpath(breakdown + "//li[contains(., 'İş emri bulunamadı')]/span[contains(@class, 'badge')]").string("173"))
                .andExpect(xpath(breakdown + "//a[contains(., 'İş emri bulunamadı')]/@href").string(containsString("/line/1?")))
                .andExpect(xpath(breakdown + "//a[contains(., 'İş emri bulunamadı')]/@href").string(containsString("status=ERROR")))
                .andExpect(xpath(breakdown + "//a[contains(., 'İş emri bulunamadı')]/@href").string(containsString("errorCode=ORA-20100")))
                // Açılır liste hattaki hata türlerini sunar
                .andExpect(xpath("//select[@name='errorCode']/option[@value='CONNECTION']").exists());
    }

    @Test
    @DisplayName("GET /line/1?errorCode=ORA-20124&page=1: tazeleme, sayfalama ve sıralama bağlantıları errorCode'u korur; seçim listede işaretli")
    void errorCodeKeptInRefreshPagingAndSortLinks() throws Exception {
        mvc.perform(get("/line/1").param("errorCode", "ORA-20124").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string(containsString("errorCode=ORA-20124")))
                .andExpect(xpath("//a[contains(text(), 'Önceki')]/@href").string(containsString("errorCode=ORA-20124")))
                .andExpect(xpath("//a[contains(text(), 'Sonraki')]/@href").string(containsString("errorCode=ORA-20124")))
                .andExpect(xpath("//table/thead//a/@href").string(containsString("errorCode=ORA-20124")))
                .andExpect(xpath("//select[@name='errorCode']/option[@selected]/@value").string("ORA-20124"));
    }

    @Test
    @DisplayName("GET /line/1?sort=asc ile ?sort=desc: Farklı ilk barkodu basar")
    void sortDirectionChangesOrder() throws Exception {
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
    void invalidParamsFallBackToDefaults() throws Exception {
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
    @DisplayName("GET /line/1?page=abc: 400 döner ve web sayfası JSON (problem+json) değil HTML hata sayfasına düşer")
    void badParamOnWebPageIsNotJson() throws Exception {
        mvc.perform(get("/line/1").param("page", "abc").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotEquals(
                        MediaType.APPLICATION_PROBLEM_JSON_VALUE, result.getResponse().getContentType()));
    }

    @Test
    @DisplayName("GET /line/1?barcodeQuery=GU132&page=1: Sayfalama bağlantılarında barcodeQuery=GU132 parametresi korunur")
    void pagingLinksKeepFilters() throws Exception {
        mvc.perform(get("/line/1")
                        .param("barcodeQuery", "GU132")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(xpath("//a[contains(text(), 'Önceki')]/@href").string(containsString("barcodeQuery=GU132")))
                .andExpect(xpath("//a[contains(text(), 'Sonraki')]/@href").string(containsString("barcodeQuery=GU132")));
    }
}
