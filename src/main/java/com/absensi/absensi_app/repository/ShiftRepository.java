package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.Shift;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @EntityGraph(attributePaths = "waktuKerja")
    List<Shift> findAll();

    /**
     * Semua shift aktif milik OPD
     */
    List<Shift> findByOpdIdAndAktifTrueOrderByNamaAsc(Long opdId);

    /**
     * Semua shift milik OPD (termasuk nonaktif)
     */
    List<Shift> findByOpdIdOrderByNamaAsc(Long opdId);

    // Backward compatibility
    default List<Shift> findByOpdIdAndAktifTrue(Long opdId) {
        return findByOpdIdAndAktifTrueOrderByNamaAsc(opdId);
    }

    @Query("""
SELECT DISTINCT s
FROM Shift s
JOIN FETCH s.opd
LEFT JOIN FETCH s.waktuKerja w
WHERE s.opd.id = :opdId
ORDER BY w.jamMasuk ASC
""")
    List<Shift> findByOpdIdOrderByJamMasukAsc(@Param("opdId") Long opdId);

    List<Shift> findByAktifTrue();
}