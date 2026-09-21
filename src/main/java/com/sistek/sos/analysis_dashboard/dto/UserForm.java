package com.sistek.sos.analysis_dashboard.dto;

import java.util.Set;

/**
 * Kullanıcı oluşturma ve düzenleme form modeli (Record).
 * Formdan gelen verileri bağlar; sunucu doğrulaması UserAdminService içinde yapılır.
 *
 * @param username Kullanıcı adı (yeni kullanıcı için zorunlu, düzenlemede readonly)
 * @param password Parola (yeni kullanıcıda en az 12 karakter zorunlu, düzenlemede boş bırakılırsa mevcut parola korunur)
 * @param roles Seçilen roller kümesi (en az bir rol zorunlu, yalnız ADMIN, USER, APIUSER)
 * @param enabled Kullanıcının etkin durumu
 */
public record UserForm(
        String username,
        String password,
        Set<String> roles,
        boolean enabled
) {
    public UserForm {
        username = (username == null) ? "" : username.strip();
        password = (password == null) ? "" : password;
        roles = (roles == null) ? Set.of() : roles;
    }
}
