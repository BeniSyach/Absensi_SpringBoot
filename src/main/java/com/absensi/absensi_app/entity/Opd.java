package com.absensi.absensi_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "opd")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String kode;

    @Column(nullable = false, length = 200)
    private String nama;

    @Column(length = 500)
    private String alamat;

    // Koordinat kantor (pusat lokasi absen)
    @Column(name = "latitude_kantor", nullable = false)
    private Double latitudeKantor;

    @Column(name = "longitude_kantor", nullable = false)
    private Double longitudeKantor;

    // Radius toleransi absen dalam meter
    @Column(name = "radius_absen", nullable = false)
    @Builder.Default
    private Integer radiusAbsen = 100;

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = true;

    @OneToMany(mappedBy = "opd", fetch = FetchType.LAZY)
    private List<User> users;

    @OneToMany(mappedBy = "opd", fetch = FetchType.LAZY)
    private List<Shift> shifts;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
