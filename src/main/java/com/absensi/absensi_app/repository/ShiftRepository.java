package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    /** Semua shift aktif milik OPD — untuk dropdown pilih shift di Android */
    List<Shift> findByOpdIdAndAktifTrueOrderByJamMasukAsc(Long opdId);

    /** Untuk admin — semua shift termasuk nonaktif */
    List<Shift> findByOpdIdOrderByJamMasukAsc(Long opdId);

    // Alias untuk backward compatibility dengan kode lama
    default List<Shift> findByOpdIdAndAktifTrue(Long opdId) {
        return findByOpdIdAndAktifTrueOrderByJamMasukAsc(opdId);
    }
}
