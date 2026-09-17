package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record BarcodeId(
        @Column(name = "barcode", nullable = false) String barcode,
        @Column(name = "line_id", nullable = false) String lineId
) implements Serializable {}
