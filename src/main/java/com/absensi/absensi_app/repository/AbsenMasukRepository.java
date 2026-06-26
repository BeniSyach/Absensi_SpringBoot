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

    @Query("SELECT a FROM AbsenMasuk a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.shift " +
            "WHERE a.opd.id = :opdId AND a.tanggal = :tanggal ORDER BY a.waktuMasuk")
    List<AbsenMasuk> findByOpdIdAndTanggal(@Param("opdId") Long opdId, @Param("tanggal") LocalDate tanggal);

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

    @Query("SELECT COUNT(a) FROM AbsenMasuk a WHERE a.user.id = :userId " +
            "AND a.tanggal BETWEEN :dari AND :sampai")
    long countByUserIdAndPeriode(@Param("userId") Long userId,
                                 @Param("dari") LocalDate dari,
                                 @Param("sampai") LocalDate sampai);
}

