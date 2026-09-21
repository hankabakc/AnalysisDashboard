package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * Denetim kaydı JPA entity sınıfı.
 * Sistemdeki kritik veri değişiklikleri ve oturum olayları burada saklanır.
 */
@Getter
@Entity
@Table(name = "app_audit_log", schema = "public")
public class AppAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "event", nullable = false, length = 30)
    private String event;

    @Column(name = "actor", length = 50)
    private String actor;

    @Column(name = "target", length = 50)
    private String target;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    public AppAuditLog() {
    }

    public AppAuditLog(Instant occurredAt, String event, String actor, String target, String oldValue, String newValue) {
        this.occurredAt = occurredAt;
        this.event = event;
        this.actor = actor;
        this.target = target;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
