package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.AbsenMasuk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbsenMasukRepository extends JpaRepository<AbsenMasuk, Long>,
        JpaSpecificationExecutor<AbsenMasuk> {

    Optional<AbsenMasuk> findByUserIdAndTanggal(Long userId, LocalDate tanggal);

    boolean existsByUserIdAndTanggal(Long userId, LocalDate tanggal);

    List<AbsenMasuk> findByUserIdAndTanggalBetweenOrderByTanggalDesc(
            Long userId, LocalDate dari, LocalDate sampai);

    @Query("""
    SELECT a FROM AbsenMasuk a
    LEFT JOIN FETCH a.user
    LEFT JOIN FETCH a.shift
    LEFT JOIN FETCH a.opd
    WHERE a.opd.id = :opdId
      AND a.tanggal BETWEEN :dari AND :sampai
    ORDER BY a.tanggal ASC, a.waktuMasuk ASC
""")
    List<AbsenMasuk> findByOpdIdAndTanggalBetween(
            @Param("opdId") Long opdId,
            @Param("dari") LocalDate dari,
            @Param("sampai") LocalDate sampai
    );

    /**
     * Cari absen masuk user yang belum punya absen pulang.
     * Digunakan untuk kasus shift malam lintas hari —
     * absen masuk bisa dari hari kemarin.
     */
    @Query("""
        SELECT a FROM AbsenMasuk a
        WHERE a.user.id = :userId
          AND a.tanggal IN (:tanggalHariIni, :tanggalKemarin)
          AND NOT EXISTS (
              SELECT p FROM AbsenPulang p WHERE p.absenMasuk.id = a.id
          )
        ORDER BY a.waktuMasuk DESC
    """)
    List<AbsenMasuk> findAbsenAktifTanpaPulang(
            @Param("userId") Long userId,
            @Param("tanggalHariIni") LocalDate tanggalHariIni,
            @Param("tanggalKemarin") LocalDate tanggalKemarin);

    @Query("""
        SELECT a FROM AbsenMasuk a
        LEFT JOIN FETCH a.user
        LEFT JOIN FETCH a.shift
        WHERE a.opd.id = :opdId
          AND a.tanggal = :tanggal
        ORDER BY a.waktuMasuk
    """)
    List<AbsenMasuk> findByOpdIdAndTanggal(
            @Param("opdId") Long opdId,
            @Param("tanggal") LocalDate tanggal);

    @Query("""
        SELECT COUNT(a) FROM AbsenMasuk a
        WHERE a.user.id = :userId
          AND a.tanggal BETWEEN :dari AND :sampai
    """)
    long countByUserIdAndPeriode(
            @Param("userId") Long userId,
            @Param("dari") LocalDate dari,
            @Param("sampai") LocalDate sampai);

    /** Hitung terlambat dalam periode untuk rekap */
    @Query("""
        SELECT COUNT(a) FROM AbsenMasuk a
        WHERE a.user.id = :userId
          AND a.tanggal BETWEEN :dari AND :sampai
          AND a.status = 'TERLAMBAT'
    """)
    long countTerlambatByUserIdAndPeriode(
            @Param("userId") Long userId,
            @Param("dari") LocalDate dari,
            @Param("sampai") LocalDate sampai);
}

