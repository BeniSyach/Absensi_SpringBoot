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

    // Toleransi terlambat dalam menit
    @Column(name = "toleransi_terlambat")
    @Builder.Default
    private Integer toleransiTerlambat = 15;

    // Toleransi pulang awal dalam menit
    @Column(name = "toleransi_pulang_awal")
    @Builder.Default
    private Integer toleransiPulangAwal = 10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opd_id", nullable = false)
    private Opd opd;

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = true;

    @OneToMany(mappedBy = "shift", fetch = FetchType.LAZY)
    private List<WaktuKerja> waktuKerjaList;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}