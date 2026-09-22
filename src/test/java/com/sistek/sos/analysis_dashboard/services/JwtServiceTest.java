package com.sistek.sos.analysis_dashboard.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JWT imzalama anahtarının uzunluk kontrolü.
 * Kısa anahtar uygulamayı açılışta durdurmalıdır; aksi hâlde hata ancak ilk girişte ortaya çıkar.
 */
class JwtServiceTest {

    private static final String VALID_SECRET = "en-az-otuziki-karakterlik-gecerli-anahtar";
    private static final String SHORT_SECRET = "kisa-anahtar";

    @Test
    @DisplayName("32 karakterden kısa jwt.secret ile servis oluşturulamaz")
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(SHORT_SECRET, 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(SHORT_SECRET));
    }

    @Test
    @DisplayName("Yeterli uzunlukta jwt.secret ile servis oluşur")
    void validSecretIsAccepted() {
        assertThatCode(() -> new JwtService(VALID_SECRET, 3600)).doesNotThrowAnyException();
    }
}
