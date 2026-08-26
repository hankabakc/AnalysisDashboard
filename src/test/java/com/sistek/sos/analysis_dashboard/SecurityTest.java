package com.sistek.sos.analysis_dashboard;

import com.sistek.sos.analysis_dashboard.entities.AppUser;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Spring Security kimlik doğrulama, yetkilendirme ve rol matrisi testleri (T-014).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Kimlik doğrulamasız GET /dashboard: 302 döner ve /login adresine yönlendirir")
    void kimliksizDashboardLoginYollar() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /login: Kimlik doğrulamasız 200 OK döner")
    void loginSayfasiHerkeseAciktir() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Analysis Dashboard")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("@WithMockUser(roles='USER'): GET /dashboard 200 döner, GET /line/1 200 ve 50 satır + sayfalama basar")
    void userRoluDashboardVeHatSayfasinaErisir() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk());

        mvc.perform(get("/line/1"))
                .andExpect(status().isOk())
                .andExpect(xpath("//table/tbody/tr").nodeCount(50));
    }

    @Test
    @WithMockUser(roles = "APIUSER")
    @DisplayName("@WithMockUser(roles='APIUSER'): GET /dashboard 403 Forbidden döner (API kullanıcısı web sayfası açamaz)")
    void apiUserRoluWebSayfasiAcamaz() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("@WithMockUser(roles='ADMIN'): GET /dashboard 200 ve GET /v3/api-docs 200 döner")
    void adminRoluHerYereErisir() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk());

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("@WithMockUser(roles='USER'): GET /v3/api-docs 403 Forbidden döner (Swagger/OpenAPI yalnız ADMIN)")
    void userRoluOpenApiyeErisemez() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Veritabanındaki parola BCrypt ile doğrulanır (düz metin saklanmadığı kanıtlanır)")
    void parolaBcryptIleDogrulanir() {
        AppUser admin = appUserRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("admin kullanıcısı veritabanında bulunamadı"));

        assertThat(admin.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("admin123", admin.getPassword())).isTrue();
    }
}
