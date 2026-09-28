package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "plc_info")
public class PlcInfo {

    @Id
    @Column(name = "plc_id", nullable = false)
    private String plcId;

    @Column(name = "status", nullable = false)
    private String status;

    /** Toplayıcının son sinyali (UTC); ~5 sn'de bir güncellenir. */
    @Column(name = "last_status_date", nullable = false)
    private LocalDateTime lastStatusDate;
}
