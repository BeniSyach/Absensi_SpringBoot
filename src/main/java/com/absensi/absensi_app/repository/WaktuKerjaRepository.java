package com.absensi.absensi_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.absensi.absensi_app.entity.WaktuKerja;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaktuKerjaRepository extends JpaRepository<WaktuKerja, Long> {

    Optional<WaktuKerja> findByShiftIdAndHariAndAktifTrue(
            Long shiftId,
            DayOfWeek hari
    );

    @Query("SELECT w FROM WaktuKerja w WHERE w.shift.id = :shiftId")
    List<WaktuKerja> findByShiftId(@Param("shiftId") Long shiftId);

    List<WaktuKerja> findByShiftIdOrderByHariAsc(Long shiftId);

    @Query("""
        SELECT w
        FROM WaktuKerja w
        JOIN w.shift s
        JOIN User u ON u.shift.id = s.id
        WHERE u.id = :userId
        AND w.hari = :hari
        AND w.aktif = true
        AND s.aktif = true
    """)
    Optional<WaktuKerja> findAktifByUserAndHari(
            @Param("userId") Long userId,
            @Param("hari") DayOfWeek hari
    );
}
