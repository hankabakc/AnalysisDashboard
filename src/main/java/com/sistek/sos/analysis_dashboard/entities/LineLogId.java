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
public class LineLogId implements Serializable {

    @Column(name = "line_id", nullable = false)
    private String lineId;

    @Column(name = "seq_no", nullable = false)
    private Long seqNo;

    public LineLogId(String lineId, Long seqNo) {
        this.lineId = lineId;
        this.seqNo = seqNo;
    }
}
