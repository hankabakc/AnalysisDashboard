package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "line_info")
public class LineInfo {

    @Id
    @Column(name = "line_id", nullable = false)
    private String lineId;

    @Column(name = "status", nullable = false)
    private String status;
}
