package com.absensi.absensi_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Hari-hari kerja dalam seminggu (MONDAY, TUESDAY, ...)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "waktu_kerja_hari", joinColumns = @JoinColumn(name = "waktu_kerja_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "hari")
    private Set<DayOfWeek> hariKerja;

    @Column(name = "tanggal_mulai", nullable = false)
    private LocalDate tanggalMulai;

    @Column(name = "tanggal_selesai")
    private LocalDate tanggalSelesai;

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}