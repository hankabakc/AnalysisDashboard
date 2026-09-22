package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.listeners.SessionAuditListener;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpSessionEvent;
import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import com.sistek.sos.analysis_dashboard.dto.AuditRow;
import com.sistek.sos.analysis_dashboard.entities.AppAuditLog;
import com.sistek.sos.analysis_dashboard.repositories.AppAuditLogRepository;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import com.sistek.sos.analysis_dashboard.services.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-017 / T-017-Eksik: Denetim Kaydı (Audit Log) ve Ekran Testleri.
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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SessionAuditListener sessionAuditListener;

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
        String allFields = String.valueOf(failureLog.getOldValue()) + failureLog.getNewValue();
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
    @DisplayName("7. /dashboard + /fragments/dashboard + /api/lines çağrıları -> app_audit_log satır sayısı değişmez")
    void readOnlyRequests_doNotGenerateAuditLogs() throws Exception {
        long countBefore = appAuditLogRepository.count();

        // 1. Web panosu oturumlu istekleri
        mvc.perform(get("/dashboard").with(user("user").roles("USER"))).andExpect(status().isOk());
        mvc.perform(get("/fragments/dashboard").with(user("user").roles("USER"))).andExpect(status().isOk());

        // 2. Geçerli APIUSER Bearer token ile REST API isteği (200 OK)
        String apiToken = createJwtToken("apiuser", List.of("ROLE_APIUSER"));
        mvc.perform(get("/api/lines")
                        .header("Authorization", "Bearer " + apiToken))
                .andExpect(status().isOk());

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
        mvc.perform(get("/admin/audit").with(user("normalUser").roles("USER")))
                .andExpect(status().isForbidden());

        // ADMIN rolü -> 200, sayfada Denetim Kaydı görünür
        MvcResult adminResult = mvc.perform(get("/admin/audit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Denetim Kaydı")))
                .andExpect(content().string(containsString("ZAMAN")))
                .andExpect(content().string(containsString("OLAY")))
                .andReturn();

        // Sıralama doğrulaması: Listenin ilk satırı veritabanındaki en yeni (en yüksek id'li) kayıt olmalı
        Page<?> logsPage = (Page<?>) adminResult.getModelAndView().getModel().get("logs");
        assertThat(logsPage).isNotEmpty();
        AuditRow firstRow = (AuditRow) logsPage.getContent().get(0);

        AppAuditLog newestLog = appAuditLogRepository.findAll().stream()
                .max(Comparator.comparing(AppAuditLog::getId))
                .orElseThrow();
        assertThat(firstRow.id()).isEqualTo(newestLog.getId());

        // HTML'de ilk satırın etiketi yer almalı
        String htmlContent = adminResult.getResponse().getContentAsString();
        assertThat(htmlContent).contains(firstRow.eventLabel());

        // ?size=9999 gönderilse bile tavan 200 olarak sınırlandırılır
        mvc.perform(get("/admin/audit")
                        .with(user("admin").roles("ADMIN"))
                        .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("logs"))
                .andExpect(result -> {
                    Page<?> page = (Page<?>) result.getModelAndView().getModel().get("logs");
                    assertThat(page.getSize()).isEqualTo(200);
                });
    }

    @Test
    @Order(9)
    @DisplayName("9. Başarılı POST /api/auth/login -> tam 1 LOGIN_SUCCESS; gövdede parola geçmez")
    void apiLoginSuccess_createsLoginSuccessEvent_withoutPassword() throws Exception {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_SUCCESS".equals(l.getEvent()) && "apiuser".equals(l.getActor()))
                .count();

        String payload = """
                {
                    "username": "apiuser",
                    "password": "apiuser123"
                }
                """;

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        List<AppAuditLog> successLogs = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_SUCCESS".equals(l.getEvent()) && "apiuser".equals(l.getActor()))
                .toList();

        assertThat(successLogs).hasSize((int) beforeCount + 1);

        AppAuditLog successLog = successLogs.get(successLogs.size() - 1);
        assertThat(successLog.getActor()).isEqualTo("apiuser");
        assertThat(successLog.getTarget()).isNull();

        String allFields = String.valueOf(successLog.getOldValue()) + successLog.getNewValue();
        assertThat(allFields).doesNotContain("apiuser123");
    }

    @Test
    @Order(10)
    @DisplayName("10. Yanlış parola ile POST /api/auth/login -> tam 1 LOGIN_FAILURE; actor = denenen kullanıcı adı; parola hiçbir alanda geçmez")
    void apiLoginFailure_createsLoginFailureEvent_withoutPassword() throws Exception {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()) && "apiuser".equals(l.getActor()))
                .count();

        String payload = """
                {
                    "username": "apiuser",
                    "password": "YanlisParola123!"
                }
                """;

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        List<AppAuditLog> failureLogs = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()) && "apiuser".equals(l.getActor()))
                .toList();

        assertThat(failureLogs).hasSize((int) beforeCount + 1);

        AppAuditLog failureLog = failureLogs.get(failureLogs.size() - 1);
        assertThat(failureLog.getActor()).isEqualTo("apiuser");
        assertThat(failureLog.getTarget()).isNull();

        String allFields = String.valueOf(failureLog.getOldValue()) + failureLog.getNewValue();
        assertThat(allFields).doesNotContain("YanlisParola123!");
    }

    @Test
    @Order(11)
    @DisplayName("11. 60 karakterlik kullanıcı adıyla POST /login -> 302 /login?error ve actor <= 50 karakterle 1 LOGIN_FAILURE")
    void longUsernameLoginFailure_redirectsToLoginError_andTruncatesActor() throws Exception {
        String longUsername = "a".repeat(60);
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()))
                .count();

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", longUsername)
                        .param("password", "YanlisParola123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        List<AppAuditLog> failures = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()))
                .toList();

        assertThat(failures).hasSize((int) beforeCount + 1);
        AppAuditLog lastFailure = failures.get(failures.size() - 1);
        assertThat(lastFailure.getActor()).isNotNull();
        assertThat(lastFailure.getActor().length()).isLessThanOrEqualTo(50);
        assertThat(lastFailure.getActor()).isEqualTo("a".repeat(50));
    }

    @Test
    @Order(12)
    @DisplayName("12. 60 karakterlik kullanıcı adıyla POST /api/auth/login -> 401 Unauthorized + problem+json (500 değil)")
    void longUsernameApiLoginFailure_returns401ProblemJson() throws Exception {
        String longUsername = "b".repeat(60);
        String payload = String.format("""
                {
                    "username": "%s",
                    "password": "YanlisParola123!"
                }
                """, longUsername);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        List<AppAuditLog> failures = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGIN_FAILURE".equals(l.getEvent()) && "b".repeat(50).equals(l.getActor()))
                .toList();
        assertThat(failures).hasSize(1);
    }

    @Test
    @Order(13)
    @DisplayName("13. Denetim kaydında admin ve apiuser varken ?query=% tüm kayıtları döndürmez")
    void queryWithWildcardPercent_doesNotMatchAllRecords() throws Exception {
        MvcResult allResult = mvc.perform(get("/admin/audit")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        Page<?> allPage = (Page<?>) allResult.getModelAndView().getModel().get("logs");
        long totalAll = allPage.getTotalElements();
        assertThat(totalAll).isGreaterThan(0);

        MvcResult percentResult = mvc.perform(get("/admin/audit")
                        .with(user("admin").roles("ADMIN"))
                        .param("query", "%"))
                .andExpect(status().isOk())
                .andReturn();
        Page<?> percentPage = (Page<?>) percentResult.getModelAndView().getModel().get("logs");
        long totalPercent = percentPage.getTotalElements();

        assertThat(totalPercent).isLessThan(totalAll);
    }

    @Test
    @Order(14)
    @DisplayName("14. USER rolü GET /admin/users çağırır -> 403 Forbidden ve 1 ACCESS_DENIED (actor=user, target=/admin/users)")
    void userAccessingAdminEndpoint_generatesAccessDeniedAuditLog() throws Exception {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "ACCESS_DENIED".equals(l.getEvent()) && "user".equals(l.getActor()))
                .count();

        mvc.perform(get("/admin/users").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());

        List<AppAuditLog> logs = appAuditLogRepository.findAll().stream()
                .filter(l -> "ACCESS_DENIED".equals(l.getEvent()) && "user".equals(l.getActor()))
                .toList();

        assertThat(logs).hasSize((int) beforeCount + 1);
        AppAuditLog log = logs.get(logs.size() - 1);
        assertThat(log.getActor()).isEqualTo("user");
        assertThat(log.getTarget()).isEqualTo("/admin/users");
        assertThat(log.getOldValue()).isNull();
        assertThat(log.getNewValue()).isNull();
    }

    @Test
    @Order(15)
    @DisplayName("15. APIUSER rolü GET /dashboard çağırır -> 403 Forbidden ve 1 ACCESS_DENIED (actor=apiuser, target=/dashboard)")
    void apiUserAccessingWebDashboard_generatesAccessDeniedAuditLog() throws Exception {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "ACCESS_DENIED".equals(l.getEvent()) && "apiuser".equals(l.getActor()))
                .count();

        mvc.perform(get("/dashboard").with(user("apiuser").roles("APIUSER")))
                .andExpect(status().isForbidden());

        List<AppAuditLog> logs = appAuditLogRepository.findAll().stream()
                .filter(l -> "ACCESS_DENIED".equals(l.getEvent()) && "apiuser".equals(l.getActor()))
                .toList();

        assertThat(logs).hasSize((int) beforeCount + 1);
        AppAuditLog log = logs.get(logs.size() - 1);
        assertThat(log.getActor()).isEqualTo("apiuser");
        assertThat(log.getTarget()).isEqualTo("/dashboard");
        assertThat(log.getOldValue()).isNull();
        assertThat(log.getNewValue()).isNull();
    }

    @Test
    @Order(16)
    @DisplayName("16. USER JWT token ile GET /api/lines -> 403 ProblemDetail ve 1 ACCESS_DENIED (actor=user, target=/api/lines)")
    void userJwtAccessingRestApi_generatesAccessDeniedAuditLog() throws Exception {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "ACCESS_DENIED".equals(l.getEvent()) && "user".equals(l.getActor()))
                .count();

        String token = createJwtToken("user", List.of("ROLE_USER"));
        mvc.perform(get("/api/lines")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Erişim Reddedildi"));

        List<AppAuditLog> logs = appAuditLogRepository.findAll().stream()
                .filter(l -> "ACCESS_DENIED".equals(l.getEvent()) && "user".equals(l.getActor()))
                .toList();

        assertThat(logs).hasSize((int) beforeCount + 1);
        AppAuditLog log = logs.get(logs.size() - 1);
        assertThat(log.getActor()).isEqualTo("user");
        assertThat(log.getTarget()).isEqualTo("/api/lines");
        assertThat(log.getOldValue()).isNull();
        assertThat(log.getNewValue()).isNull();
    }

    @Test
    @Order(17)
    @DisplayName("17. Anonim istekler (/dashboard, /fragments/dashboard, /line/1) 0 denetim kaydı üretir (tablo şişmesi önlenir)")
    void anonymousRequests_produceZeroAuditLogs() throws Exception {
        long beforeCount = appAuditLogRepository.count();

        // 1. GET /dashboard -> 302 yönlendirme
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());

        // 2. GET /fragments/dashboard (htmx header ile) -> 401
        mvc.perform(get("/fragments/dashboard").header("HX-Request", "true")).andExpect(status().isUnauthorized());

        // 3. GET /fragments/dashboard (normal) -> 302 yönlendirme
        mvc.perform(get("/fragments/dashboard")).andExpect(status().is3xxRedirection());

        // 4. GET /line/1 -> 302 yönlendirme
        mvc.perform(get("/line/1")).andExpect(status().is3xxRedirection());

        long afterCount = appAuditLogRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    @Order(18)
    @DisplayName("18. POST /logout ile oturum kapatıldığında tam 1 LOGOUT oluşur, 0 SESSION_EXPIRED oluşur")
    void logout_generatesOnlyLogout_notSessionExpired() throws Exception {
        // Oturum aç
        MvcResult loginResult = mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "user")
                        .param("password", "user123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        long beforeLogout = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGOUT".equals(l.getEvent()) && "user".equals(l.getActor()))
                .count();
        long beforeExpired = appAuditLogRepository.findAll().stream()
                .filter(l -> "SESSION_EXPIRED".equals(l.getEvent()) && "user".equals(l.getActor()))
                .count();

        // Çıkış yap
        mvc.perform(post("/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection());

        // LOGOUT_IN_PROGRESS_ATTR işaretli oturum kapandığında da SESSION_EXPIRED oluşmadığı doğrulanır
        MockHttpSession mockLogoutSession = new MockHttpSession();
        mockLogoutSession.setAttribute(SessionAuditListener.LOGOUT_IN_PROGRESS_ATTR, Boolean.TRUE);
        SecurityContextImpl logoutContext =
                new SecurityContextImpl(
                        new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        mockLogoutSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                logoutContext);
        sessionAuditListener.sessionDestroyed(new HttpSessionEvent(mockLogoutSession));

        long afterLogout = appAuditLogRepository.findAll().stream()
                .filter(l -> "LOGOUT".equals(l.getEvent()) && "user".equals(l.getActor()))
                .count();
        long afterExpired = appAuditLogRepository.findAll().stream()
                .filter(l -> "SESSION_EXPIRED".equals(l.getEvent()) && "user".equals(l.getActor()))
                .count();

        assertThat(afterLogout).isEqualTo(beforeLogout + 1);
        assertThat(afterExpired).isEqualTo(beforeExpired);
    }

    @Test
    @Order(19)
    @DisplayName("19. Oturum zaman aşımı / geçersiz kılınması -> SecurityContext üzerinden 1 SESSION_EXPIRED (actor=username, target=null)")
    void sessionTimeout_generatesSessionExpiredEvent() {
        MockHttpSession session = new MockHttpSession();
        SecurityContextImpl securityContext =
                new SecurityContextImpl(
                        new UsernamePasswordAuthenticationToken(
                                "zamanAsimiKullanici",
                                "n/a",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                );
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "SESSION_EXPIRED".equals(l.getEvent()) && "zamanAsimiKullanici".equals(l.getActor()))
                .count();

        sessionAuditListener.sessionDestroyed(new HttpSessionEvent(session));

        List<AppAuditLog> logs = appAuditLogRepository.findAll().stream()
                .filter(l -> "SESSION_EXPIRED".equals(l.getEvent()) && "zamanAsimiKullanici".equals(l.getActor()))
                .toList();

        assertThat(logs).hasSize((int) beforeCount + 1);
        AppAuditLog log = logs.get(logs.size() - 1);
        assertThat(log.getActor()).isEqualTo("zamanAsimiKullanici");
        assertThat(log.getTarget()).isNull();
        assertThat(log.getOldValue()).isNull();
        assertThat(log.getNewValue()).isNull();
    }

    @Test
    @Order(20)
    @DisplayName("20. GET /admin/audit: ACCESS_DENIED 'Yetkisiz Erişim ⛔' ve SESSION_EXPIRED 'Oturum Süresi Doldu ⏱️' olarak basılır")
    void auditScreen_rendersTurkishLabelsAndIconsForNewEvents() throws Exception {
        mvc.perform(get("/admin/audit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yetkisiz Erişim")))
                .andExpect(content().string(containsString("⛔")))
                .andExpect(content().string(containsString("Oturum Süresi Doldu")))
                .andExpect(content().string(containsString("⏱️")));
    }

    @Test
    @Order(21)
    @DisplayName("21. Oturumda SecurityContext yoksa veya anonim kullanıcıysa sessionDestroyed çağrısı 0 SESSION_EXPIRED üretir")
    void sessionDestroyed_withoutSecurityContextOrAnonymous_doesNotGenerateAuditLog() {
        long beforeCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "SESSION_EXPIRED".equals(l.getEvent()))
                .count();

        // 1. Hiçbir güvenlik bağlamı olmayan oturum
        MockHttpSession emptySession = new MockHttpSession();
        sessionAuditListener.sessionDestroyed(new HttpSessionEvent(emptySession));

        // 2. anonymousUser oturumu
        MockHttpSession anonSession = new MockHttpSession();
        SecurityContextImpl anonContext =
                new SecurityContextImpl(
                        new UsernamePasswordAuthenticationToken(
                                "anonymousUser",
                                "n/a",
                                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                        )
                );
        anonSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                anonContext
        );
        sessionAuditListener.sessionDestroyed(new HttpSessionEvent(anonSession));

        long afterCount = appAuditLogRepository.findAll().stream()
                .filter(l -> "SESSION_EXPIRED".equals(l.getEvent()))
                .count();

        assertThat(afterCount).isEqualTo(beforeCount);
    }

    private String createJwtToken(String username, List<String> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                authorities.stream().map(SimpleGrantedAuthority::new).toList()
        );
        return jwtService.generateToken(auth);
    }
}

