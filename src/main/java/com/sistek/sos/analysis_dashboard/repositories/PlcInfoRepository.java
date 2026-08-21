package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.PlcInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlcInfoRepository extends JpaRepository<PlcInfo, String> {
    Optional<PlcInfo> findFirstByOrderByPlcIdAsc();
}
