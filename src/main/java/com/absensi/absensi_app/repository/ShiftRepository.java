package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByOpdIdAndAktifTrue(Long opdId);
}
