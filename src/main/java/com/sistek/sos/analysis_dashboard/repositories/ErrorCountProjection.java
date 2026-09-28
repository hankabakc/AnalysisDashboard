package com.sistek.sos.analysis_dashboard.repositories;

/**
 * Hata dağılımı sorgusunun bir satırı: ERP hata kodu (null olabilir) ve o koddaki barkod adedi.
 */
public interface ErrorCountProjection {
    String getCode();
    long getCount();
}
