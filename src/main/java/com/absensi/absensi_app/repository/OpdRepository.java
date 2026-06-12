package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.Opd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpdRepository extends JpaRepository<Opd, Long> {
    Optional<Opd> findByKode(String kode);
    List<Opd> findByAktifTrue();
}
