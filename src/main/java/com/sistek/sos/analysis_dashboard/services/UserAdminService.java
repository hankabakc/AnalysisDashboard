package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.UserForm;
import com.sistek.sos.analysis_dashboard.dto.UserRow;
import com.sistek.sos.analysis_dashboard.entities.AppUser;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.exceptions.UserBusinessException;
import com.sistek.sos.analysis_dashboard.exceptions.UserValidationException;
import com.sistek.sos.analysis_dashboard.repositories.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Kullanıcı yönetimi servisi.
 * Tek yazma noktasıdır; listeleme, ekleme, güncelleme, silme ve tüm iş kuralları (kilitlenme koruması dahil) burada işletilir.
 */
@Service
@Transactional
public class UserAdminService {

    public static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "USER", "APIUSER");
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]+$");
    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Kullanıcıları kullanıcı adına göre artan sıralı ve sayfalanmış olarak listeler.
     */
    @Transactional(readOnly = true)
    public Page<UserRow> getUsers(PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.ASC, "username"));
        return appUserRepository.findAll(pageable)
                .map(u -> new UserRow(u.getUsername(), u.getRoles(), u.isEnabled()));
    }


    /**
     * Düzenleme ekranı için kullanıcının mevcut bilgilerini DTO olarak döner (parola alanı boş döner).
     */
    @Transactional(readOnly = true)
    public UserForm getUserForEdit(String username) {
        AppUser user = appUserRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + username));
        return new UserForm(user.getUsername(), "", user.getRoles(), user.isEnabled());
    }

    /**
     * Yeni kullanıcı oluşturur.
     */
    public void createUser(UserForm form) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Kullanıcı adı kontrolü
        if (form.username().isBlank()) {
            errors.put("username", "Kullanıcı adı zorunludur.");
        } else if (form.username().length() < MIN_USERNAME_LENGTH || form.username().length() > MAX_USERNAME_LENGTH) {
            errors.put("username", "Kullanıcı adı 3 ile 50 karakter arasında olmalıdır.");
        } else if (!USERNAME_PATTERN.matcher(form.username()).matches()) {
            errors.put("username", "Kullanıcı adı yalnızca küçük harf (a-z), rakam (0-9), nokta (.), alt çizgi (_) ve tire (-) içerebilir.");
        } else if (appUserRepository.existsById(form.username())) {
            errors.put("username", "Bu kullanıcı adı zaten kullanılıyor.");
        }

        // Parola kontrolü: oluşturmada zorunlu ve en az 12 karakter
        if (form.password().isBlank()) {
            errors.put("password", "Parola zorunludur.");
        } else if (form.password().length() < MIN_PASSWORD_LENGTH) {
            errors.put("password", "Parola en az 12 karakter olmalıdır.");
        }

        // Rol kontrolü: en az bir rol ve yalnızca izin verilen roller
        validateRoles(form.roles(), errors);

        if (!errors.isEmpty()) {
            throw new UserValidationException(errors);
        }

        String passwordHash = passwordEncoder.encode(form.password());
        AppUser user = new AppUser(form.username(), passwordHash, form.enabled(), form.roles());
        appUserRepository.save(user);
    }

    /**
     * Var olan kullanıcıyı günceller.
     */
    public void updateUser(String username, UserForm form, String currentUsername) {
        AppUser user = appUserRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + username));

        Map<String, String> errors = new LinkedHashMap<>();

        // Parola kontrolü: güncellemede boş bırakılabilir; doluysa en az 12 karakter olmalıdır
        if (!form.password().isBlank() && form.password().length() < MIN_PASSWORD_LENGTH) {
            errors.put("password", "Parola en az 12 karakter olmalıdır.");
        }

        // Rol kontrolü
        validateRoles(form.roles(), errors);

        if (!errors.isEmpty()) {
            throw new UserValidationException(errors);
        }

        // Kilitlenme koruması (Lockout Protection)
        // 1. Kendi hesabı mı?
        if (username.equals(currentUsername)) {
            if (!form.enabled()) {
                throw new UserBusinessException("Kendi hesabınızı pasifleştiremezsiniz.");
            }
            if (!form.roles().contains("ADMIN")) {
                throw new UserBusinessException("Kendi ADMIN rolünüzü kaldıramazsınız.");
            }
        }

        // 2. Sistemdeki son etkin ADMIN mi?
        boolean isTargetActiveAdmin = user.isEnabled() && user.getRoles().contains("ADMIN");
        if (isTargetActiveAdmin) {
            boolean willRemainActiveAdmin = form.enabled() && form.roles().contains("ADMIN");
            if (!willRemainActiveAdmin) {
                long activeAdminCount = appUserRepository.countActiveAdmins();
                if (activeAdminCount <= 1) {
                    if (!form.enabled()) {
                        throw new UserBusinessException("Sistemdeki son etkin yönetici pasifleştirilemez.");
                    } else {
                        throw new UserBusinessException("Sistemdeki son etkin yöneticinin ADMIN rolü kaldırılamaz.");
                    }
                }
            }
        }

        // Değişiklikleri uygula
        if (!form.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.password()));
        }
        user.setEnabled(form.enabled());
        user.setRoles(form.roles());
        appUserRepository.save(user);
    }

    /**
     * Kullanıcıyı siler.
     */
    public void deleteUser(String username, String currentUsername) {
        AppUser user = appUserRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + username));

        // Kilitlenme koruması
        if (username.equals(currentUsername)) {
            throw new UserBusinessException("Kendi hesabınızı silemezsiniz.");
        }

        boolean isTargetActiveAdmin = user.isEnabled() && user.getRoles().contains("ADMIN");
        if (isTargetActiveAdmin) {
            long activeAdminCount = appUserRepository.countActiveAdmins();
            if (activeAdminCount <= 1) {
                throw new UserBusinessException("Sistemdeki son etkin yönetici silinemez.");
            }
        }

        appUserRepository.delete(user);
    }

    private void validateRoles(Set<String> roles, Map<String, String> errors) {
        if (roles == null || roles.isEmpty()) {
            errors.put("roles", "En az bir rol seçilmelidir.");
        } else if (!ALLOWED_ROLES.containsAll(roles)) {
            errors.put("roles", "Yalnızca ADMIN, USER ve APIUSER rollerine izin verilir.");
        }
    }
}
