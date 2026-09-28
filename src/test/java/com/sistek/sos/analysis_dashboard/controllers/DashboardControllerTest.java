package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import com.sistek.sos.analysis_dashboard.dto.DisplayTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    private static final String LINE_1 = "//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='1']]";
    private static final String DISPLAY_COUNT = "//div[contains(@class, 'display-6')]";
    private static final String BREAKDOWN = "//section[@aria-label='ERP Hata Dağılımı']";

    @Autowired
    private MockMvc mvc;

    /** @Transactional testlerde yazılan satırlar test bitince geri alınır; diğer testler örnek veriyi aynen görür. */
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("GET /dashboard: 192.168.1.181, ACTIVE ve 5 hattın adetlerini hat kartı bazında kesin doğrular")
    void dashboardShowsPlcAndLineCounts() throws Exception {
        mvc.perform(get("/dashboard").param("range", "all"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("192.168.1.181")))
                .andExpect(content().string(containsString("ACTIVE")))
                // XPath ile her hat kartı içindeki toplam adedin birebir eşleşmesi ve bağlantısı doğrulanır
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='1']]//div[contains(@class, 'display-6')]").string("1319"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='2']]//div[contains(@class, 'display-6')]").string("6"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='3']]//div[contains(@class, 'display-6')]").string("14"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='4']]//div[contains(@class, 'display-6')]").string("2325"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='5']]//div[contains(@class, 'display-6')]").string("2"))
                // Hat kartlarının /line/{id} bağlantıları seçili dönemi taşır
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='1']]/@href").string("/line/1?range=all&day=&shift="))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='5']]/@href").string("/line/5?range=all&day=&shift="))
                // Dış CDN kullanılmaz; varlıklar WebJar olarak pakete gömülü
                .andExpect(content().string(not(containsString("cdn.jsdelivr.net"))))
                .andExpect(content().string(containsString("/webjars/bootstrap/")));
    }

    @Test
    @DisplayName("GET /fragments/dashboard: hat kartında aktarılan/hatalı/bekleyen, altta tüm hatların ERP hata dağılımı")
    void dashboardShowsErpTransferCountsAndErrorBreakdown() throws Exception {
        mvc.perform(get("/fragments/dashboard").param("range", "all"))
                .andExpect(status().isOk())
                .andExpect(xpath(LINE_1 + "//dt[contains(., 'Aktarılan')]/following-sibling::dd[1]").string("0"))
                .andExpect(xpath(LINE_1 + "//dt[contains(., 'Hatalı')]/following-sibling::dd[1]").string("1319"))
                .andExpect(xpath(LINE_1 + "//dt[contains(., 'Bekleyen')]/following-sibling::dd[1]").string("0"))
                .andExpect(xpath(BREAKDOWN +"//li").nodeCount(3))
                .andExpect(xpath(BREAKDOWN +"//li[contains(., 'İş emri bulunamadı')]/span[contains(@class, 'badge')]").string("2505"))
                .andExpect(xpath(BREAKDOWN +"//li[contains(., 'Barkod formatı hatalı')]/span[contains(@class, 'badge')]").string("961"))
                .andExpect(xpath(BREAKDOWN +"//li[contains(., 'ulaşılamadı')]/span[contains(@class, 'badge')]").string("200"))
                // Tüm hatların özetinde süzülecek tek hat yok; satırlar bağlantı değildir
                .andExpect(xpath(BREAKDOWN +"//a").doesNotExist());
    }

    @Test
    @DisplayName("Toplayıcı canlı (son sinyal 3 sn önce): uyarı yok; PLC kartında son sinyal, hat kartında son barkod yerel saatle")
    void liveCollectorShowsLastSignalWithoutWarning() throws Exception {
        mvc.perform(get("/fragments/dashboard"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@role='alert']").doesNotExist())
                .andExpect(xpath("//section[@aria-label='PLC Bilgisi']").string(containsString(
                        "Son sinyal: " + DisplayTime.format(LocalDateTime.parse("2025-07-31T09:42:56.856763")) + " (3 sn önce)")))
                .andExpect(xpath(LINE_1 + "//p[contains(., 'Son barkod')]").string(containsString(
                        DisplayTime.format(LocalDateTime.parse("2025-07-31T09:42:46.956361")))));
    }

    @Test
    @Transactional
    @DisplayName("Son sinyal 13 dk önce: panoda 'veri gelmiyor' uyarısı süreyi ve durumların son bilinen değer olduğunu yazar")
    void silentCollectorShowsWarning() throws Exception {
        jdbc.update("UPDATE plc_info SET last_status_date = '2025-07-31 09:30:00'");
        mvc.perform(get("/fragments/dashboard"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@role='alert'][contains(., 'veri gelmiyor')]").string(containsString("13 dk")))
                .andExpect(xpath("//div[@role='alert'][contains(., 'veri gelmiyor')]").string(containsString("son bilinen")));
    }

    @Test
    @Transactional
    @DisplayName("5 dk'dan uzun süredir NEW bekleyen barkod uyarıda sayılır; 30 sn'lik NEW sayılmaz ama bekleyende görünür")
    void stuckNewBarcodesAreCounted() throws Exception {
        jdbc.update("""
                INSERT INTO barcode_data (barcode, line_id, cre_date, status) VALUES
                ('TEST-ESKI', '1', '2025-07-31 09:37:00', 'NEW'),
                ('TEST-YENI', '1', '2025-07-31 09:42:30', 'NEW')
                """);
        mvc.perform(get("/fragments/dashboard"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@role='alert'][contains(., 'gönderilmeyi bekliyor')]").string(containsString("1 barkod")))
                .andExpect(xpath("//div[@role='alert'][contains(., 'gönderilmeyi bekliyor')]").string(containsString("5 dk")))
                .andExpect(xpath(LINE_1 + "//dt[contains(., 'Bekleyen')]/following-sibling::dd[1]").string("2"));
    }

    @Test
    @DisplayName("Varsayılan dönem bu vardiya: Hat 1 = 197 (hepsi bağlantı hatası), etiket vardiyayı yazar")
    void defaultPeriodIsCurrentShift() throws Exception {
        mvc.perform(get("/fragments/dashboard"))
                .andExpect(status().isOk())
                .andExpect(xpath(LINE_1 + DISPLAY_COUNT).string("197"))
                .andExpect(content().string(containsString("Bu vardiya: 31.07.2025 · 1. vardiya (08:00–16:00)")))
                .andExpect(xpath(BREAKDOWN + "//li").nodeCount(1))
                .andExpect(xpath(BREAKDOWN + "//li[contains(., 'ulaşılamadı')]/span[contains(@class, 'badge')]").string("197"));
    }

    @Test
    @DisplayName("?range=today → Hat 1 197; ?day=2025-07-28 → Hat 1 209, Hat 4 460; &shift=2 → 16 / 33")
    void todayAndSpecificDay() throws Exception {
        String line4 = "//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='4']]";
        mvc.perform(get("/fragments/dashboard").param("range", "today"))
                .andExpect(xpath(LINE_1 + DISPLAY_COUNT).string("197"));
        mvc.perform(get("/fragments/dashboard").param("day", "2025-07-28"))
                .andExpect(xpath(LINE_1 + DISPLAY_COUNT).string("209"))
                .andExpect(xpath(line4 + DISPLAY_COUNT).string("460"))
                .andExpect(content().string(containsString("28.07.2025 · tüm gün (08:00–08:00)")));
        mvc.perform(get("/fragments/dashboard").param("day", "2025-07-28").param("shift", "2"))
                .andExpect(xpath(LINE_1 + DISPLAY_COUNT).string("16"))
                .andExpect(xpath(line4 + DISPLAY_COUNT).string("33"));
    }

    @Test
    @DisplayName("GET /dashboard?day=2025-07-28&shift=2: tazeleme ve hat bağlantıları dönemi taşır, form seçimi gösterir; hazır dönemler günü bırakır")
    void periodIsKeptInRefreshAndLineLinks() throws Exception {
        mvc.perform(get("/dashboard").param("day", "2025-07-28").param("shift", "2"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string("/fragments/dashboard?range=shift&day=2025-07-28&shift=2"))
                .andExpect(xpath(LINE_1 + "/@href").string("/line/1?range=shift&day=2025-07-28&shift=2"))
                .andExpect(xpath("//input[@name='day']/@value").string("2025-07-28"))
                .andExpect(xpath("//select[@name='shift']/option[@selected]/@value").string("2"))
                .andExpect(xpath("//nav[@aria-label='Hazır dönemler']//a[@aria-current]").doesNotExist())
                .andExpect(xpath("//a[text()='Bu vardiya']/@href").string("/dashboard?range=shift"))
                .andExpect(xpath("//a[text()='Tüm zamanlar']/@href").string("/dashboard?range=all"));
    }

    @Test
    @DisplayName("GET /dashboard: veri alanı /fragments/dashboard ile tazelenir; aralık 5 sn, bayatlık 15 sn, zaman aşımı 4 sn; uyarı başta gizli")
    void dashboardPageIsWiredForRefresh() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-get").string("/fragments/dashboard?range=shift&day=&shift="))
                .andExpect(xpath("//div[@hx-trigger='refresh']/@hx-sync").string("this:drop"))
                .andExpect(xpath("//nav[@aria-label='Hazır dönemler']//a[@aria-current='true']").string("Bu vardiya"))
                .andExpect(xpath("//script[contains(@src, '/webjars/htmx.org/')]").exists())
                .andExpect(xpath("//script[@src='/js/refresh.js']/@data-refresh-ms").string("5000"))
                .andExpect(xpath("//script[@src='/js/refresh.js']/@data-stale-ms").string("15000"))
                .andExpect(xpath("//script[@src='/js/refresh.js']/@data-timeout-ms").string("4000"))
                .andExpect(xpath("//div[@id='stale-banner']/@hidden").exists())
                .andExpect(xpath("//div[@id='stale-banner']").string(containsString("Veriler güncel değil")));
    }

    @Test
    @DisplayName("GET /fragments/dashboard: yalnızca veri alanını (PLC + hat adetleri + son güncelleme) döner; menü ve sayfa iskeleti yok")
    void dashboardFragmentRendersDataOnly() throws Exception {
        mvc.perform(get("/fragments/dashboard").param("range", "all"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("192.168.1.181")))
                .andExpect(content().string(containsString("Son güncelleme:")))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='1']]//div[contains(@class, 'display-6')]").string("1319"))
                .andExpect(xpath("//a[contains(@class, 'card flex-shrink-0')][.//h4[text()='4']]//div[contains(@class, 'display-6')]").string("2325"))
                .andExpect(content().string(not(containsString("<nav"))))
                .andExpect(content().string(not(containsString("<head"))))
                .andExpect(content().string(not(containsString("refresh.js"))));
    }
}
