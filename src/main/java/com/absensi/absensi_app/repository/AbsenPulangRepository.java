package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.AbsenPulang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbsenPulangRepository extends JpaRepository<AbsenPulang, Long>,
        JpaSpecificationExecutor<AbsenPulang> {

    Optional<AbsenPulang> findByUserIdAndTanggal(Long userId, LocalDate tanggal);

    boolean existsByUserIdAndTanggal(Long userId, LocalDate tanggal);

    List<AbsenPulang> findByUserIdAndTanggalBetweenOrderByTanggalDesc(
            Long userId, LocalDate dari, LocalDate sampai);

    Optional<AbsenPulang> findByAbsenMasukId(Long absenMasukId);

    @Query("SELECT a FROM AbsenPulang a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.shift " +
            "WHERE a.opd.id = :opdId AND a.tanggal = :tanggal ORDER BY a.waktuPulang")
    List<AbsenPulang> findByOpdIdAndTanggal(@Param("opdId") Long opdId, @Param("tanggal") LocalDate tanggal);
}
