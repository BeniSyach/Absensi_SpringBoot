package com.absensi.absensi_app.dto.response;

import com.absensi.absensi_app.enums.StatusAbsensi;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbsenResponse {
    private Long id;
    private String jenis; // MASUK / PULANG
    private LocalDateTime waktu;
    private Double latitude;
    private Double longitude;
    private Double jarakDariKantor;
    private Boolean lokasiValid;
    private Boolean mockLocationDetected;
    private String fotoAbsen;
    private StatusAbsensi status;
    private String pesan;
    private Integer durasiKerjaMenit; // hanya untuk pulang
    private UserResponse user;

    // Info shift yang dipakai
    private Long shiftId;
    private String shiftNama;

    private Long waktuKerjaId;
    private String hari;
    private LocalTime jamMasuk;
    private LocalTime jamPulang;
    private Integer toleransiTerlambat;
    private Integer toleransiPulangAwal;
    private Boolean lintasHari;
}

