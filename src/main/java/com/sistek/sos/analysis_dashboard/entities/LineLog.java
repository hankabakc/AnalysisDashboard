package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "line_log")
public class LineLog {

    @EmbeddedId
    private LineLogId id;

    @Column(nullable = false)
    private LocalDateTime procDate;

    @Column(nullable = false)
    private String status;
}
