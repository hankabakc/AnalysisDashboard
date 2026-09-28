package com.sistek.sos.analysis_dashboard.config;

import com.sistek.sos.analysis_dashboard.dto.UserForm;
import com.sistek.sos.analysis_dashboard.exceptions.UserValidationException;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import com.sistek.sos.analysis_dashboard.services.UserAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Canlı ortamda ilk yönetici. Geliştirme kullanıcıları yalnızca dev profilinde eklenir; canlıda hiç kullanıcı yoksa kimse
 * giremezdi. Etkin bir ADMIN yoksa ve DASHBOARD_INITIAL_ADMIN_PASSWORD verilmişse açılışta "admin" hesabı açılır.
 *
 * Parola koda ve depoya girmez (ortam değişkeni). Kural ve denetim kaydı kullanıcı ekranındakiyle aynıdır
 * (UserAdminService.createUser); işlemi yapan bir kişi olmadığı için denetim kaydında actor boştur.
 * Etkin bir ADMIN varsa hiçbir şey yapılmaz: hesap açıldıktan sonra değişken kaldırılmalı, parola ekrandan değiştirilmelidir.
 */
@Component
public class InitialAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdmin.class);
    private static final String USERNAME = "admin";

    private final AppUserRepository appUserRepository;
    private final UserAdminService userAdminService;
    private final String password;

    public InitialAdmin(AppUserRepository appUserRepository, UserAdminService userAdminService,
                        @Value("${dashboard.initial-admin-password:}") String password) {
        this.appUserRepository = appUserRepository;
        this.userAdminService = userAdminService;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank() || appUserRepository.countActiveAdmins() > 0) {
            return;
        }
        try {
            userAdminService.createUser(new UserForm(USERNAME, password, Set.of("ADMIN"), true), null);
            log.warn("İlk yönetici '{}' oluşturuldu. Parolayı Kullanıcılar ekranından değiştirin ve "
                    + "DASHBOARD_INITIAL_ADMIN_PASSWORD değişkenini kaldırın.", USERNAME);
        } catch (UserValidationException e) {
            // Açılışı düşürmek panoyu da kapatırdı; hesap açılmadığı açıkça günlüğe yazılır
            log.error("İlk yönetici oluşturulamadı ({}): {}", USERNAME, e.getMessage());
        }
    }
}
