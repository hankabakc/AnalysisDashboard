package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import com.sistek.sos.analysis_dashboard.entities.AppUser;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-016: Kullanıcı Yönetimi (ADMIN) Denetleyici ve İş Kuralı Testleri.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class UserAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("1. admin ile GET /admin/users: 200 döner, listede admin, user, apiuser görünür")
    void adminCanListUsers() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("user")))
                .andExpect(content().string(containsString("apiuser")))
                .andExpect(content().string(containsString("Kullanıcı Yönetimi")));
    }

    @Test
    @DisplayName("2a. Oturumsuz GET /admin/users: 302 döner ve /login'e yönlendirir")
    void anonymousGetsRedirectToLogin() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("2b. user rolü ile GET /admin/users: 403 Forbidden döner")
    void userRoleGets403Forbidden() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "apiuser", roles = "APIUSER")
    @DisplayName("2c. apiuser rolü ile GET /admin/users: 403 Forbidden döner")
    void apiUserRoleGets403Forbidden() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("3. POST /admin/users (hat.operator, 12+ parola, USER): 302 döner, DB'de $2a$ ile başlar")
    void createValidUserSucceeds() throws Exception {
        String testUser = "hat.operator";
        appUserRepository.deleteById(testUser);

        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", "GuvenliParola123")
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        Optional<AppUser> created = appUserRepository.findById(testUser);
        assertThat(created).isPresent();
        assertThat(created.get().getPassword()).startsWith("$2a$");
        assertThat(created.get().getRoles()).containsExactly("USER");
        assertThat(created.get().isEnabled()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("4. 11 karakterlik parola ile POST /admin/users: 200 döner, 'en az 12' mesajı çıkar, kullanıcı oluşmaz")
    void shortPasswordFailsValidation() throws Exception {
        String testUser = "kisa.parola";
        appUserRepository.deleteById(testUser);

        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", "12345678901") // 11 karakter
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("en az 12")));

        assertThat(appUserRepository.existsById(testUser)).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("5. Var olan kullanıcı adıyla ekleme: kullanıcı sayısı değişmez, alan mesajı görünür")
    void duplicateUsernameFailsValidation() throws Exception {
        long countBefore = appUserRepository.count();

        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "GuvenliParola123")
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("zaten kullanılıyor")));

        assertThat(appUserRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("6. Rol SUPERADMIN verilirse: reddedilir, kullanıcı oluşmaz")
    void invalidRoleFailsValidation() throws Exception {
        String testUser = "super.user";
        appUserRepository.deleteById(testUser);

        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", "GuvenliParola123")
                        .param("roles", "SUPERADMIN")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yalnızca ADMIN, USER, APIUSER rollerine izin verilir.")));

        assertThat(appUserRepository.existsById(testUser)).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("7. Güncellemede parola boşsa hash değişmez, doluysa değişir ve yeni parolayla giriş yapılır")
    void updatePasswordBehavior() throws Exception {
        String testUser = "guncelleme.test";
        appUserRepository.deleteById(testUser);

        // İlk oluşturma
        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", "IlkParola12345")
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection());

        AppUser initial = appUserRepository.findById(testUser).orElseThrow();
        String initialHash = initial.getPassword();

        // 7a. Parola boş bırakılarak güncelleme: hash değişmez
        mvc.perform(post("/admin/users/" + testUser)
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", "") // Boş bırakıldı
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AppUser afterEmptyUpdate = appUserRepository.findById(testUser).orElseThrow();
        assertThat(afterEmptyUpdate.getPassword()).isEqualTo(initialHash);

        // 7b. Parola dolu (12+) verilerek güncelleme: hash değişir ve yeni parolayla giriş yapılabilir
        String newPassword = "YeniGucluParola999";
        mvc.perform(post("/admin/users/" + testUser)
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", newPassword)
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AppUser afterPasswordUpdate = appUserRepository.findById(testUser).orElseThrow();
        assertThat(afterPasswordUpdate.getPassword()).isNotEqualTo(initialHash);
        assertThat(afterPasswordUpdate.getPassword()).startsWith("$2a$");

        // Yeni parolayla form login denemesi
        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("8. admin kendini silmeye çalışırsa silinmez; tek etkin ADMIN iken rolünü USER yapamaz")
    void adminCannotDeleteSelfOrDemoteLastAdmin() throws Exception {
        // 8a. admin kendini silmeye çalışır -> silinmez
        mvc.perform(post("/admin/users/admin/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertThat(appUserRepository.existsById("admin")).isTrue();

        // 8b. Tek etkin ADMIN iken kendi rolünü USER yapmaya çalışır -> reddedilir
        mvc.perform(post("/admin/users/admin")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "")
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kendi ADMIN rolünüzü kaldıramazsınız.")));

        AppUser adminUser = appUserRepository.findById("admin").orElseThrow();
        assertThat(adminUser.getRoles()).contains("ADMIN");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("9. hat.operator silinir: 302; app_user ve app_user_role satırları gider")
    void deleteUserRemovesUserAndRoles() throws Exception {
        String testUser = "hat.operator";
        // Kullanıcı yoksa önce oluşturalım
        if (!appUserRepository.existsById(testUser)) {
            mvc.perform(post("/admin/users")
                            .with(csrf())
                            .param("username", testUser)
                            .param("password", "ParolaUzun12345")
                            .param("roles", "USER")
                            .param("enabled", "true"))
                    .andExpect(status().is3xxRedirection());
        }
        assertThat(appUserRepository.existsById(testUser)).isTrue();

        // Silme işlemi
        mvc.perform(post("/admin/users/" + testUser + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertThat(appUserRepository.existsById(testUser)).isFalse();
        assertThat(appUserRepository.findById(testUser)).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("10. CSRF token'sız POST /admin/users: 403 Forbidden döner, kullanıcı oluşmaz")
    void postWithoutCsrfGets403Forbidden() throws Exception {
        String testUser = "no.csrf.user";
        appUserRepository.deleteById(testUser);

        mvc.perform(post("/admin/users")
                        // with(csrf()) YOK
                        .param("username", testUser)
                        .param("password", "GuvenliParola123")
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isForbidden());

        assertThat(appUserRepository.existsById(testUser)).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("11. GET /admin/users/new ve GET /admin/users/{username}/edit form render testleri: 200 döner")
    void formRenderTests() throws Exception {
        mvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yeni Kullanıcı Ekle")))
                .andExpect(content().string(not(containsString("th:utext"))));

        mvc.perform(get("/admin/users/admin/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kullanıcıyı Düzenle")))
                .andExpect(content().string(containsString("value=\"admin\"")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("12. admin, user hesabını pasifleştirir: 302; DB'de enabled=false, listede '▲ Pasif' görünür, kullanıcı giriş yapamaz")
    void adminCanDisableUser() throws Exception {
        // user hesabını pasifleştir (checkbox işaretsiz: _enabled=on)
        mvc.perform(post("/admin/users/user")
                        .with(csrf())
                        .param("username", "user")
                        .param("password", "")
                        .param("roles", "USER")
                        .param("_enabled", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AppUser disabledUser = appUserRepository.findById("user").orElseThrow();
        assertThat(disabledUser.isEnabled()).isFalse();

        // Listede ▲ Pasif göründüğünü doğrula
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("▲ Pasif")));

        // Pasifleştirilen kullanıcı giriş yapamaz (DisabledException -> 302 /login?error)
        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "user")
                        .param("password", "user123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));


        // Temizlik: user'ı tekrar aktif yapalım ki diğer testleri etkilemesin
        disabledUser.setEnabled(true);
        appUserRepository.save(disabledUser);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("13. admin kendi hesabını pasifleştiremez: 200 döner, hata mesajı basılır, DB değişmez")
    void adminCannotDisableSelf() throws Exception {
        mvc.perform(post("/admin/users/admin")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "")
                        .param("roles", "ADMIN")
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kendi hesabınızı pasifleştiremezsiniz.")));

        AppUser adminUser = appUserRepository.findById("admin").orElseThrow();
        assertThat(adminUser.isEnabled()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin2", roles = "ADMIN")
    @DisplayName("14. Sistemdeki son etkin ADMIN pasifleştirilemez: reddedilir, DB değişmez")
    void cannotDisableLastActiveAdmin() throws Exception {
        // Sistemde tek etkin ADMIN 'admin' var.
        // admin2 mock kullanıcısı 'admin'i pasifleştirmeye çalışır:
        mvc.perform(post("/admin/users/admin")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "")
                        .param("roles", "ADMIN")
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sistemdeki son etkin yönetici pasifleştirilemez.")));

        AppUser adminUser = appUserRepository.findById("admin").orElseThrow();
        assertThat(adminUser.isEnabled()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("15. Başında ve sonunda boşluk bulunan parolayla oluşturma: parola kırpılmaz, boşluklu parolayla form login başarılı olur")
    void passwordWithLeadingAndTrailingSpacesPreserved() throws Exception {
        String testUser = "bosluklu.parola";
        String passwordWithSpaces = "  GizliParola123!  "; // başında ve sonunda boşluk olan 19 karakter
        appUserRepository.deleteById(testUser);

        // 1. Kullanıcıyı oluştur
        mvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", passwordWithSpaces)
                        .param("roles", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertThat(appUserRepository.existsById(testUser)).isTrue();

        // 2. Kırpılmış parolayla giriş başarısız olmalı (parola kırpılmadığı için eşleşmez)
        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", passwordWithSpaces.trim()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // 3. Tam olarak girilen (başında ve sonunda boşluk olan) parolayla giriş başarılı olmalı
        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testUser)
                        .param("password", passwordWithSpaces))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}

