package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Uygulama kullanıcı varlığı.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "username"))
    @Column(name = "role", nullable = false, length = 50)
    private Set<String> roles = new HashSet<>();

    public AppUser(String username, String password, boolean enabled, Set<String> roles) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.roles = roles != null ? roles : new HashSet<>();
    }
}
