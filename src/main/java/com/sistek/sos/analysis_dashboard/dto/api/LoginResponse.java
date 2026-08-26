package com.sistek.sos.analysis_dashboard.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Başarılı giriş sonrası JWT yanıt modeli.
 */
@Schema(description = "JWT erişim belirteci yanıtı")
public record LoginResponse(
        @Schema(description = "JWT Bearer erişim belirteci")
        String token,

        @Schema(description = "Belirteç türü", example = "Bearer")
        String tokenType,

        @Schema(description = "Belirtecin saniye cinsinden geçerlilik süresi", example = "3600")
        long expiresIn
) {
}
