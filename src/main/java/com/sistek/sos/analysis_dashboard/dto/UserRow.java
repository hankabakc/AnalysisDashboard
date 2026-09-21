package com.sistek.sos.analysis_dashboard.dto;

import java.util.Set;

/**
 * Kullanıcı yönetimi liste görünüm modeli (Record).
 * Şablona entity verilmemesi (WEB-07 §1.3) ve parola/hash sızdırılmaması (WEB-02 §4.1) kuralına uygundur.
 *
 * @param username Kullanıcı adı
 * @param roles Kullanıcıya atanmış roller
 * @param enabled Kullanıcının aktif/pasif durumu
 */
public record UserRow(
        String username,
        Set<String> roles,
        boolean enabled
) {
}
