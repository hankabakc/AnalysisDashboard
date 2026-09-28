package com.sistek.sos.analysis_dashboard.repositories;

/**
 * Saatlik üretim sorgusunun bir satırı: dönem başından itibaren kaçıncı saat ve o saatte okunan barkodların
 * ERP durumuna göre dağılımı.
 */
public interface HourlyCountProjection {
    int getHourIndex();
    long getTotal();
    long getSent();
    long getError();
    long getPending();
}
