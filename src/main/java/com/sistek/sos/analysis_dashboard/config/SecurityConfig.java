package com.sistek.sos.analysis_dashboard.config;

import com.sistek.sos.analysis_dashboard.dto.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistek.sos.analysis_dashboard.listeners.SessionAuditListener;
import com.sistek.sos.analysis_dashboard.services.AuditService;
import com.sistek.sos.analysis_dashboard.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;
import java.net.URI;

/**
 * Spring Security güvenlik yapılandırması:
 * 1. Zincir (API): /api/** yolları için stateless, JWT tabanlı koruma.
 * 2. Zincir (Web): HTML ekranları için oturum ve form login koruması.
 *
 * Kullanıcılar tek UserDetailsService bean'inden (AppUserDetailsService) otomatik bağlanır.
 * JWT "roles" iddiası → ROLE_ yetkileri eşlemesi application.properties'tedir.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public SecurityConfig(JwtService jwtService, ObjectMapper objectMapper, AuditService auditService) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * REST API Güvenlik Filtre Zinciri (Stateless, JWT).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().hasAnyRole("ADMIN", "APIUSER")
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtService.getJwtDecoder()))
                .authenticationEntryPoint((request, response, e) -> writeProblem(request, response,
                        HttpStatus.UNAUTHORIZED, "Yetkilendirme Gerekli",
                        "Bu uca erişmek için geçerli bir Bearer token gereklidir."))
                .accessDeniedHandler((request, response, e) -> {
                    String actor = AuditService.extractAuthenticatedActor(SecurityContextHolder.getContext().getAuthentication());
                    if (actor != null) {
                        auditService.record(AuditEvent.ACCESS_DENIED, actor, request.getRequestURI(), null, null);
                    }
                    writeProblem(request, response,
                            HttpStatus.FORBIDDEN, "Erişim Reddedildi",
                            "Bu işlem için gerekli yetkiye sahip değilsiniz.");
                })
            );

        return http.build();
    }

    /**
     * Web Arayüzü Güvenlik Filtre Zinciri (Stateful, Form Login).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint loginPage = new LoginUrlAuthenticationEntryPoint("/login");

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/webjars/**", "/error").permitAll()
                .requestMatchers("/admin/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                .requestMatchers("/dashboard", "/line/**", "/fragments/**", "/").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
            )
            // Oturum yoksa normal istek giriş sayfasına yönlenir. htmx tazeleme isteği (HX-Request) ise 401 alır;
            // yönlenseydi giriş sayfasının HTML'i veri alanının içine basılırdı. refresh.js 401'de sayfayı yeniler.
            // Yetkisiz erişimlerde ise ACCESS_DENIED denetim kaydı oluşturulur ve 403 döner (T-020).
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    if ("true".equals(request.getHeader("HX-Request"))) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    } else {
                        loginPage.commence(request, response, e);
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String actor = AuditService.extractAuthenticatedActor(SecurityContextHolder.getContext().getAuthentication());
                    if (actor != null) {
                        auditService.record(AuditEvent.ACCESS_DENIED, actor, request.getRequestURI(), null, null);
                    }
                    response.sendError(HttpStatus.FORBIDDEN.value());
                })
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .addLogoutHandler((request, response, authentication) -> {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.setAttribute(SessionAuditListener.LOGOUT_IN_PROGRESS_ATTR, Boolean.TRUE);
                    }
                    String actor = AuditService.extractAuthenticatedActor(authentication);
                    if (actor != null) {
                        auditService.record(AuditEvent.LOGOUT, actor, null, null, null);
                    }
                })
                .permitAll()
            );

        return http.build();
    }

    /** API'nin 401/403 yanıtlarını 404 ile aynı RFC 7807 ProblemDetail gövdesiyle yazar. */
    private void writeProblem(HttpServletRequest request, HttpServletResponse response,
                              HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
