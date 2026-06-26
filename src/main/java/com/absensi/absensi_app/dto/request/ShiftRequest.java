package com.absensi.absensi_app.dto.request;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
public class ShiftRequest {
    private String nama;
    private LocalTime jamMasuk;
    private LocalTime jamPulang;

    private Integer toleransiTerlambat;
    private Integer toleransiPulangAwal;

    private Boolean aktif;

    private Long opdId;

    // optional
    private Long userId;

    private Set<DayOfWeek> hariKerja;

    private LocalDate tanggalMulai;

    private LocalDate tanggalSelesai;
}
