package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.LineInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LineInfo varlığı için veri erişim katmanı (SRP & Repository Pattern).
 * Yalnızca line_info tablosundan ve LineInfo varlığının yaşam döngüsünden sorumludur.
 */
@Repository
public interface LineInfoRepository extends JpaRepository<LineInfo, String> {

    List<LineInfo> findAllByOrderByLineIdAsc();
}
