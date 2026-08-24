package com.sistek.sos.analysis_dashboard.dto.api;

/**
 * PLC bilgisi REST yanıt modeli (T-008).
 * Güvenlik/ürün kuralı: plcIp hiçbir şekilde yer almaz.
 */
public record PlcResponse(
        String id,
        String status
) {}
