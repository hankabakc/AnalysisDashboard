package com.sistek.sos.analysis_dashboard.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kullanıcı girişi istek modeli.
 */
@Schema(description = "Kullanıcı adı ve parola ile kimlik doğrulama isteği")
public record LoginRequest(
        @Schema(description = "Kullanıcı adı", example = "apiuser")
        String username,

        @Schema(description = "Parola", example = "apiuser123")
        String password
) {
}
