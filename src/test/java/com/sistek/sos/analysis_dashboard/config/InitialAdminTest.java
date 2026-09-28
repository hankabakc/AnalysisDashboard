package com.sistek.sos.analysis_dashboard.config;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import com.sistek.sos.analysis_dashboard.entities.AppUser;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import com.sistek.sos.analysis_dashboard.services.UserAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Canlı ortamda ilk yönetici. Her test kendi işleminde çalışır ve sonunda geri alınır; dev kullanıcıları diğer testler
 * için yerinde kalır.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class InitialAdminTest {

    private static final String PASSWORD = "Uzun-ve-guclu-parola-42";

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private void run(String password) {
        new InitialAdmin(appUserRepository, userAdminService, password).run(null);
    }

    @Test
    @DisplayName("Etkin ADMIN yokken ve parola verilmişken 'admin' hesabı ADMIN rolüyle açılır; parola eşleşir")
    void createsAdminWhenNoneExists() {
        jdbc.update("DELETE FROM app_user WHERE username = 'admin'");
        assertEquals(0, appUserRepository.countActiveAdmins());

        run(PASSWORD);

        AppUser admin = appUserRepository.findById("admin").orElseThrow();
        assertTrue(admin.isEnabled());
        assertTrue(admin.getRoles().contains("ADMIN"));
        assertTrue(passwordEncoder.matches(PASSWORD, admin.getPassword()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM app_audit_log WHERE event = 'USER_CREATED' AND target = 'admin' AND actor IS NULL", Long.class));
    }

    @Test
    @DisplayName("Etkin 'admin' varsa dokunmaz: parolası değişmez")
    void leavesExistingAdminAlone() {
        String before = appUserRepository.findById("admin").orElseThrow().getPassword();
        run(PASSWORD);
        assertEquals(before, appUserRepository.findById("admin").orElseThrow().getPassword());
    }

    @Test
    @DisplayName("Yönetici başka adla (yonetici) varsa fazladan 'admin' hesabı açılmaz")
    void doesNotAddASecondAdmin() {
        jdbc.update("INSERT INTO app_user (username, password, enabled) VALUES ('yonetici', 'x', true)");
        jdbc.update("INSERT INTO app_user_role (username, role) VALUES ('yonetici', 'ADMIN')");
        jdbc.update("DELETE FROM app_user WHERE username = 'admin'");
        run(PASSWORD);
        assertFalse(appUserRepository.existsById("admin"));
    }

    @Test
    @DisplayName("Kural dışı (12 karakterden kısa) parola: hesap açılmaz, uygulama açılışı düşmez; parola yoksa hiçbir şey olmaz")
    void invalidOrMissingPasswordCreatesNothing() {
        jdbc.update("DELETE FROM app_user WHERE username = 'admin'");
        run("kisa");
        assertFalse(appUserRepository.existsById("admin"));
        run("");
        assertFalse(appUserRepository.existsById("admin"));
    }
}
