package com.absensi.absensi_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "waktu_kerja")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaktuKerja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(nullable = false, length = 50)
    private String hari;

    private LocalTime jamMasuk;

    private LocalTime jamPulang;

    private Integer toleransiTerlambat;

    private Integer toleransiPulangAwal;

    private Boolean lintasHari;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}