package com.absensi.absensi_app.entity;

import com.absensi.absensi_app.enums.StatusAbsensi;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absen_pulang", indexes = {
        @Index(name = "idx_absen_pulang_user_tanggal", columnList = "user_id, tanggal"),
        @Index(name = "idx_absen_pulang_tanggal", columnList = "tanggal"),
        @Index(name = "idx_absen_pulang_absen_masuk", columnList = "absen_masuk_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenPulang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opd_id", nullable = false)
    private Opd opd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waktu_kerja_id")
    private WaktuKerja waktuKerja;

    // Relasi ke absen masuk hari ini
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "absen_masuk_id")
    private AbsenMasuk absenMasuk;

    @Column(nullable = false)
    private LocalDate tanggal;

    @Column(name = "waktu_pulang", nullable = false)
    private LocalDateTime waktuPulang;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "akurasi_gps")
    private Float akurasiGps;

    @Column(name = "jarak_dari_kantor")
    private Double jarakDariKantor;

    @Column(name = "foto_absen")
    private String fotoAbsen;

    @Column(name = "lokasi_valid", nullable = false)
    @Builder.Default
    private Boolean lokasiValid = false;

    @Column(name = "mock_location_detected")
    @Builder.Default
    private Boolean mockLocationDetected = false;

    @Column(name = "location_provider", length = 50)
    private String locationProvider;

    @Column(name = "kecepatan_perpindahan")
    private Double kecepatanPerpindahan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAbsensi status = StatusAbsensi.HADIR;

    // Durasi kerja dalam menit
    @Column(name = "durasi_kerja_menit")
    private Integer durasiKerjaMenit;

    @Column(name = "device_info", length = 200)
    private String deviceInfo;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String catatan;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
