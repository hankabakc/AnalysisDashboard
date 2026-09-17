package com.sistek.sos.analysis_dashboard.dto;

/**
 * PLC kimliği ve durumu. Hem HTML panosu hem de REST API tarafından ortak kullanılır.
 * plc_ip bilinçli olarak yer almaz.
 */
public record PlcSummary(
        String id,
        String status
) {}
