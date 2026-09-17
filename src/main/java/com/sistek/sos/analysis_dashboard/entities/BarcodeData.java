package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "barcode_data")
public class BarcodeData {

    @EmbeddedId
    private BarcodeId id;

    @Column(name = "cre_date", nullable = false)
    private LocalDateTime creDate;

    @Column(name = "status", nullable = false)
    private String status;
}
