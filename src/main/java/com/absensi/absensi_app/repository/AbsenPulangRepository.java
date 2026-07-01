package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.AbsenMasuk;
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

    @Query("""
    SELECT a FROM AbsenPulang a
    LEFT JOIN FETCH a.user
    LEFT JOIN FETCH a.shift
    LEFT JOIN FETCH a.opd
    WHERE a.opd.id = :opdId
      AND a.tanggal BETWEEN :dari AND :sampai
    ORDER BY a.tanggal ASC, a.waktuPulang ASC
""")
    List<AbsenPulang> findByOpdIdAndTanggalBetween(
            @Param("opdId") Long opdId,
            @Param("dari") LocalDate dari,
            @Param("sampai") LocalDate sampai
    );

    /** Cari pulang berdasarkan absen masuk — penting untuk shift lintas hari */
    Optional<AbsenPulang> findByAbsenMasukId(Long absenMasukId);

    List<AbsenPulang> findByUserIdAndTanggalBetweenOrderByTanggalDesc(
            Long userId, LocalDate dari, LocalDate sampai);

    @Query("""
        SELECT a FROM AbsenPulang a
        LEFT JOIN FETCH a.user
        LEFT JOIN FETCH a.shift
        WHERE a.opd.id = :opdId
          AND a.tanggal = :tanggal
        ORDER BY a.waktuPulang
    """)
    List<AbsenPulang> findByOpdIdAndTanggal(
            @Param("opdId") Long opdId,
            @Param("tanggal") LocalDate tanggal);


}
