package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kullanıcı veritabanı erişim arayüzü. Kullanıcı adı birincil anahtardır; findById yeterlidir.
 */
public interface AppUserRepository extends JpaRepository<AppUser, String> {
}
