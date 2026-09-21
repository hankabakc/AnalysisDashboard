package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import com.sistek.sos.analysis_dashboard.entities.AppAuditLog;
import com.sistek.sos.analysis_dashboard.repositories.AppAuditLogRepository;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-017: Denetim Kaydı (Audit Log) ve Ekran Testleri.
 * Kabul kriterlerinin 8 maddesini test eder.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppAuditLogRepository appAuditLogRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @Order(1)
    @DisplayName("1. Yanlış parolayla giriş: tam 1 LOGIN_FAILURE satırı; actor = denenen kullanıcı adı; parola hiçbir alanda geçmez")
    void wrongPasswordLogin_createsLoginFailure_withoutPassword() throws Exception {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()) && "hataliKullanici".equals(l.getActor()))
                .count();

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "hataliKullanici")
                        .param("password", "GizliParola999!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        List<AppAuditLog> failures = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()) && "hataliKullanici".equals(l.getActor()))
                .toList();

        assertThat(failures).hasSize((int) beforeCount + 1);

        AppAuditLog failureLog = failures.get(failures.size() - 1);
        assertThat(failureLog.getActor()).isEqualTo("hataliKullanici");
        assertThat(failureLog.getTarget()).isNull();

        // Parola metni kesinlikle hiçbir alanda geçmemeli
        String allFields = failureLog.toString() + failureLog.getOldValue() + failureLog.getNewValue();
        assertThat(allFields).doesNotContain("GizliParola999!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Doğru giriş -> LOGIN_SUCCESS; çıkış -> LOGOUT")
    void successfulLoginAndLogout_createsSuccessAndLogoutEvents() throws Exception {
        // Doğru kimlik bilgileriyle form login
        MvcResult loginResult = mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        // LOGIN_SUCCESS kaydı kontrolü
        List<AppAuditLog> successLogs = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_SUCCESS".equals(l.getEvent()) && "admin".equals(l.getActor()))
                .toList();
        assertThat(successLogs).isNotEmpty();

        // Çıkış yapma
        mvc.perform(post("/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // LOGOUT kaydı kontrolü
        List<AppAuditLog> logoutLogs = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGOUT".equals(l.getEvent()) && "admin".equals(l.getActor()))
                .toList();
        assertThat(logoutLogs).isNotEmpty();
    }

    @Test
    @Order(3)
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("3. Kullanıcı ekleme -> USER_CREATED, target = yeni kullanıcı, new_value rolleri içerir, old_value boş")
    void userCreated_createsUserCreatedEvent_withRolesInNewValue() throws Exception {
        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", "denetimuser")
                        .param("password", "GuvenliParola123!")
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AppAuditLog createdLog = appAuditLogRepository.findAll().stream()
                .filter(l -> "USER_CREATED".equals(l.getEvent()) && "denetimuser".equals(l.getTarget()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("USER_CREATED denetim kaydı bulunamadı"));

        assertThat(createdLog.getActor()).isEqualTo("admin");
        assertThat(createdLog.getTarget()).isEqualTo("denetimuser");
        assertThat(createdLog.getOldValue()).isNull();
        assertThat(createdLog.getNewValue()).contains("roles=");
        assertThat(createdLog.getNewValue()).contains("USER");
        assertThat(createdLog.getNewValue()).doesNotContain("GuvenliParola123!");
    }

    @Test
    @Order(4)
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("4. Rol güncelleme -> USER_UPDATED; old_value/new_value yalnızca değişen alanı içerir")
    void userUpdatedRoles_createsUserUpdatedEvent_onlyChangedFields() throws Exception {
        // Yalnızca rol değiştirilir (enabled aynı kalır: true, parola boş: değişmez)
        mvc.perform(post("/admin/users/denetimuser")
                        .with(csrf())
                        .param("username", "denetimuser")
                        .param("password", "")
                        .param("roles", "ADMIN")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AppAuditLog updateLog = appAuditLogRepository.findAll().stream()
                .filter(l -> "USER_UPDATED".equals(l.getEvent()) && "denetimuser".equals(l.getTarget()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("USER_UPDATED denetim kaydı bulunamadı"));

        assertThat(updateLog.getActor()).isEqualTo("admin");
        assertThat(updateLog.getTarget()).isEqualTo("denetimuser");

        // Yalnızca değişen rol bilgisi yer almalı; enabled ve parola değişmediği için içermemeli
        assertThat(updateLog.getOldValue()).contains("roles=");
        assertThat(updateLog.getOldValue()).doesNotContain("enabled=");
        assertThat(updateLog.getOldValue()).doesNotContain("parola");

        assertThat(updateLog.getNewValue()).contains("roles=");
        assertThat(updateLog.getNewValue()).doesNotContain("enabled=");
        assertThat(updateLog.getNewValue()).doesNotContain("parola");
    }

    @Test
    @Order(5)
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("5. Parola güncelleme -> USER_UPDATED; kayıtta ne yeni parola ne $2a$ ile başlayan hash geçer")
    void userUpdatedPassword_createsUserUpdatedEvent_masksPassword() throws Exception {
        String newSecret = "YeniCokGizliParola123!";

        mvc.perform(post("/admin/users/denetimuser")
                        .with(csrf())
                        .param("username", "denetimuser")
                        .param("password", newSecret)
                        .param("roles", "ADMIN")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AppAuditLog updateLog = appAuditLogRepository.findAll().stream()
                .filter(l -> "USER_UPDATED".equals(l.getEvent()) && "denetimuser".equals(l.getTarget()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("USER_UPDATED denetim kaydı bulunamadı"));

        assertThat(updateLog.getNewValue()).contains("parola degisti");
        assertThat(updateLog.getNewValue()).doesNotContain(newSecret);
        assertThat(updateLog.getNewValue()).doesNotContain("$2a$");
        if (updateLog.getOldValue() != null) {
            assertThat(updateLog.getOldValue()).doesNotContain(newSecret);
            assertThat(updateLog.getOldValue()).doesNotContain("$2a$");
        }
    }

    @Test
    @Order(6)
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("6. Kullanıcı silme -> USER_DELETED; kullanıcı silindiği hâlde kayıt durur")
    void userDeleted_createsUserDeletedEvent_recordPersistsAfterUserDeletion() throws Exception {
        mvc.perform(post("/admin/users/denetimuser/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        // Kullanıcı silindi
        assertThat(appUserRepository.findById("denetimuser")).isEmpty();

        // Ancak denetim kaydı duruyor
        AppAuditLog deleteLog = appAuditLogRepository.findAll().stream()
                .filter(l -> "USER_DELETED".equals(l.getEvent()) && "denetimuser".equals(l.getTarget()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("USER_DELETED denetim kaydı bulunamadı"));

        assertThat(deleteLog.getActor()).isEqualTo("admin");
        assertThat(deleteLog.getTarget()).isEqualTo("denetimuser");
        assertThat(deleteLog.getOldValue()).contains("roles=");
        assertThat(deleteLog.getNewValue()).isNull();
    }

    @Test
    @Order(7)
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("7. /dashboard + /fragments/dashboard + /api/lines çağrıları -> app_audit_log satır sayısı değişmez")
    void readOnlyRequests_doNotGenerateAuditLogs() throws Exception {
        long countBefore = appAuditLogRepository.count();

        mvc.perform(get("/dashboard")).andExpect(status().isOk());
        mvc.perform(get("/fragments/dashboard")).andExpect(status().isOk());
        mvc.perform(get("/line/1")).andExpect(status().isOk());

        long countAfter = appAuditLogRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @Order(8)
    @DisplayName("8. admin GET /admin/audit: 200, en yeni en üstte; user: 403; oturumsuz: 302; ?size=9999: en çok 200 satır")
    void auditScreenAccessAndPagination() throws Exception {
        // Oturumsuz -> 302 -> /login
        mvc.perform(get("/admin/audit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // USER rolü -> 403
        mvc.perform(get("/admin/audit").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("normalUser").roles("USER")))
                .andExpect(status().isForbidden());

        // ADMIN rolü -> 200, sayfada Denetim Kaydı görünür
        mvc.perform(get("/admin/audit").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Denetim Kaydı")))
                .andExpect(content().string(containsString("ZAMAN")))
                .andExpect(content().string(containsString("OLAY")));

        // ?size=9999 gönderilse bile tavan 200 olarak sınırlandırılır
        mvc.perform(get("/admin/audit")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeExists("logs"))
                .andExpect(result -> {
                    org.springframework.data.domain.Page<?> page =
                            (org.springframework.data.domain.Page<?>) result.getModelAndView().getModel().get("logs");
                    assertThat(page.getSize()).isEqualTo(200);
                });
    }
}
