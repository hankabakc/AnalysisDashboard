package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Denetim kaydı entity sınıfı (T-017).
 * Sistemdeki kritik veri değişiklikleri ve oturum olayları burada saklanır.
 * Pure Java kuralına uygun olarak yazılmıştır (Lombok yasak).
 */
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

    public AppAuditLog(Long id, Instant occurredAt, String event, String actor, String target, String oldValue, String newValue) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.event = event;
        this.actor = actor;
        this.target = target;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEvent() {
        return event;
    }

    public String getActor() {
        return actor;
    }

    public String getTarget() {
        return target;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
