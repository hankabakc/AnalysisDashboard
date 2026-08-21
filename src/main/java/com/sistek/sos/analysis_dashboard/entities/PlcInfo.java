package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "plc_info")
public class PlcInfo {

    @Id
    @Column(name = "plc_id", nullable = false)
    private String plcId;

    @Column(name = "status", nullable = false)
    private String status;
}
