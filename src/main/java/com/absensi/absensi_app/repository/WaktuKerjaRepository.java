package com.absensi.absensi_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.absensi.absensi_app.entity.WaktuKerja;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WaktuKerjaRepository extends JpaRepository<WaktuKerja, Long> {

    @Query("SELECT wk FROM WaktuKerja wk JOIN wk.hariKerja h " +
            "WHERE wk.user.id = :userId AND wk.aktif = true " +
            "AND :tanggal BETWEEN wk.tanggalMulai AND COALESCE(wk.tanggalSelesai, :tanggal) " +
            "AND h = :hari")
    Optional<WaktuKerja> findAktifByUserAndHari(
            @Param("userId") Long userId,
            @Param("tanggal") LocalDate tanggal,
            @Param("hari") DayOfWeek hari);
}
