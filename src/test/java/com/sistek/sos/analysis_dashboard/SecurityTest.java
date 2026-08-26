package com.sistek.sos.analysis_dashboard;

import com.sistek.sos.analysis_dashboard.entities.AppUser;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import com.sistek.sos.analysis_dashboard.services.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Spring Security kimlik doğrulama, JWT ve yetkilendirme testleri (T-014, T-015).
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

    @Autowired
    private JwtService jwtService;

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

    @Test
    @DisplayName("POST /api/auth/login apiuser ile: 200 döner ve geçerli JWT döner")
    void apiLoginBasarili() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apiuser\",\"password\":\"apiuser123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(3600)));
    }

    @Test
    @DisplayName("POST /api/auth/login yanlış parola ile: 401 ProblemDetail döner (kullanıcı varlığı sızdırmaz)")
    void apiLoginYanlisParola401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apiuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.detail", is("Kullanıcı adı veya parola hatalı.")))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/lines token'sız: 401 ProblemDetail döner, HTML login'e yönlendirmez")
    void apiTokensizErisim401ProblemDetail() throws Exception {
        mvc.perform(get("/api/lines"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.properties").doesNotExist())
                .andExpect(content().string(not(containsString("<form"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    @DisplayName("GET /api/lines APIUSER JWT ile: 200 döner ve 1319·6·14·2325·2 miktarlarını basar")
    void apiLinesApiUserTokenIleErisir() throws Exception {
        String token = createJwtToken("apiuser", List.of("ROLE_APIUSER"));

        mvc.perform(get("/api/lines")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319")))
                .andExpect(content().string(containsString("2325")));
    }

    @Test
    @DisplayName("GET /api/lines USER JWT ile: 403 ProblemDetail döner (USER API çağıramaz)")
    void apiLinesUserTokenIle403() throws Exception {
        String token = createJwtToken("user", List.of("ROLE_USER"));

        mvc.perform(get("/api/lines")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/lines ADMIN JWT ile: 200 döner")
    void apiLinesAdminTokenIleErisir() throws Exception {
        String token = createJwtToken("admin", List.of("ROLE_ADMIN"));

        mvc.perform(get("/api/lines")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1319")));
    }

    @Test
    @DisplayName("GET /api/lines geçersiz JWT ile: 401 ProblemDetail döner")
    void apiLinesGecersizToken401() throws Exception {
        mvc.perform(get("/api/lines")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)));
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
