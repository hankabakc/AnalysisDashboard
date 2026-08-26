package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kullanıcı veritabanı erişim arayüzü.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByUsername(String username);
}
