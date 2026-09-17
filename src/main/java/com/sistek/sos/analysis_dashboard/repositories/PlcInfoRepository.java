package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.PlcInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * PlcInfo varlığı için veri erişim katmanı.
 */
public interface PlcInfoRepository extends JpaRepository<PlcInfo, String> {

    Optional<PlcInfo> findFirstByOrderByPlcIdAsc();

    List<PlcInfo> findAllByOrderByPlcIdAsc();
}
