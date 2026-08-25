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
public class PlcLogId implements Serializable {

    @Column(name = "plc_id", nullable = false)
    private String plcId;

    @Column(name = "seq_no", nullable = false)
    private Long seqNo;

    public PlcLogId(String plcId, Long seqNo) {
        this.plcId = plcId;
        this.seqNo = seqNo;
    }
}
