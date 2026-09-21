package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Kullanıcı veritabanı erişim arayüzü. Kullanıcı adı birincil anahtardır; findById yeterlidir.
 */
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    /**
     * Kullanıcıları kullanıcı adına göre artan sıralı sayfalar.
     */
    Page<AppUser> findAllByOrderByUsernameAsc(Pageable pageable);

    /**
     * Sistemdeki aktif ADMIN rolüne sahip kullanıcı sayısını döner (kilitlenme koruması için).
     */
    @Query("SELECT COUNT(u) FROM AppUser u JOIN u.roles r WHERE u.enabled = true AND r = 'ADMIN'")
    long countActiveAdmins();
}

