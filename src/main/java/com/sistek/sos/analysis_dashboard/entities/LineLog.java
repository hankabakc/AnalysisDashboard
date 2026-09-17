package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "line_log")
public class LineLog {

    @EmbeddedId
    private LineLogId id;

    @Column(name = "proc_date", nullable = false)
    private LocalDateTime procDate;

    @Column(name = "status", nullable = false)
    private String status;
}
