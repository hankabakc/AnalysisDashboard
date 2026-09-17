package com.sistek.sos.analysis_dashboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record PlcLogId(
        @Column(name = "plc_id", nullable = false) String plcId,
        @Column(name = "seq_no", nullable = false) Long seqNo
) implements Serializable {}
