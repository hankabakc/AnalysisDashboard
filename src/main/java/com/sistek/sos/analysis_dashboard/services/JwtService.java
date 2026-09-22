package com.sistek.sos.analysis_dashboard.services;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * JWT token üretme, imzalama (HS256) ve doğrulama servisi.
 */
@Service
public class JwtService {

    /** HS256 en az 256 bitlik (32 bayt) anahtar ister. */
    private static final int MIN_SECRET_BYTES = 32;
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long expirationSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Kisa anahtar acilista degil, ilk girisde patlardi; anahtarin kendisi mesaja yazilmaz
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret en az " + MIN_SECRET_BYTES + " karakter olmalıdır (HS256 için 256 bit); verilen uzunluk: " + keyBytes.length);
        }
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Kullanıcı ve rollerini içeren imzalı JWT üretir. */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith(ROLE_PREFIX) ? auth.substring(ROLE_PREFIX.length()) : auth)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("analysis-dashboard")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public JwtDecoder getJwtDecoder() {
        return jwtDecoder;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}


