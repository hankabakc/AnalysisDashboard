package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class BarcodeId implements Serializable {

    @Column(name = "barcode", nullable = false)
    private String barcode;

    @Column(name = "line_id", nullable = false)
    private String lineId;

    public BarcodeId(String barcode, String lineId) {
        this.barcode = barcode;
        this.lineId = lineId;
    }
}
