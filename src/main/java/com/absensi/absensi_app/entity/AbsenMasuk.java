package com.absensi.absensi_app.entity;

import com.absensi.absensi_app.enums.StatusAbsensi;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absen_masuk", indexes = {
        @Index(name = "idx_absen_masuk_user_tanggal", columnList = "user_id, tanggal"),
        @Index(name = "idx_absen_masuk_tanggal", columnList = "tanggal"),
        @Index(name = "idx_absen_masuk_opd", columnList = "opd_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenMasuk {

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

    @Column(nullable = false)
    private LocalDate tanggal;

    @Column(name = "waktu_masuk", nullable = false)
    private LocalDateTime waktuMasuk;

    // Koordinat saat absen
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    // Akurasi GPS dari device (meter)
    @Column(name = "akurasi_gps")
    private Float akurasiGps;

    // Jarak dari kantor dalam meter
    @Column(name = "jarak_dari_kantor")
    private Double jarakDariKantor;

    // Foto absen (path file)
    @Column(name = "foto_absen")
    private String fotoAbsen;

    // Flag apakah lokasi valid
    @Column(name = "lokasi_valid", nullable = false)
    @Builder.Default
    private Boolean lokasiValid = false;

    // Flag mock location detection
    @Column(name = "mock_location_detected")
    @Builder.Default
    private Boolean mockLocationDetected = false;

    // Provider GPS (gps, network, fused)
    @Column(name = "location_provider", length = 50)
    private String locationProvider;

    // Kecepatan perpindahan dari titik sebelumnya (m/s) - deteksi teleportasi
    @Column(name = "kecepatan_perpindahan")
    private Double kecepatanPerpindahan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAbsensi status = StatusAbsensi.HADIR;

    // Info device
    @Column(name = "device_info", length = 200)
    private String deviceInfo;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // Catatan
    @Column(length = 500)
    private String catatan;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
