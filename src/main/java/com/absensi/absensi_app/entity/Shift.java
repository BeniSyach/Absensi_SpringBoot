package com.absensi.absensi_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shift")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nama;

    @Column(name = "jam_masuk", nullable = false)
    private LocalTime jamMasuk;

    @Column(name = "jam_pulang", nullable = false)
    private LocalTime jamPulang;

    /** Toleransi terlambat dalam menit */
    @Column(name = "toleransi_terlambat")
    @Builder.Default
    private Integer toleransiTerlambat = 15;

    /** Toleransi pulang awal dalam menit */
    @Column(name = "toleransi_pulang_awal")
    @Builder.Default
    private Integer toleransiPulangAwal = 10;

    /**
     * Flag lintas hari — true jika jam_pulang < jam_masuk
     * (contoh: masuk 20:00, pulang 04:00 keesokan harinya).
     * Diisi otomatis sebelum save, atau bisa diset manual admin.
     */
    @Column(name = "lintas_hari", nullable = false)
    @Builder.Default
    private Boolean lintasHari = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opd_id", nullable = false)
    private Opd opd;

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Hitung apakah shift ini melewati tengah malam secara otomatis.
     * Dipanggil di @PrePersist dan @PreUpdate agar selalu sinkron.
     */
    @PrePersist
    @PreUpdate
    public void hitungLintasHari() {
        if (jamMasuk != null && jamPulang != null) {
            this.lintasHari = jamPulang.isBefore(jamMasuk);
        }
    }

    /**
     * Dapatkan batas TERLAMBAT (jam masuk + toleransi).
     */
    public LocalTime batasTerlambat() {
        return jamMasuk.plusMinutes(toleransiTerlambat != null ? toleransiTerlambat : 15);
    }

    /**
     * Dapatkan batas PULANG AWAL (jam pulang - toleransi).
     */
    public LocalTime batasPulangAwal() {
        return jamPulang.minusMinutes(toleransiPulangAwal != null ? toleransiPulangAwal : 10);
    }
}