package com.absensi.absensi_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class WaktuKerjaResponse {

    private Long id;

    private String hari;

    private LocalTime jamMasuk;

    private LocalTime jamPulang;

    private Integer toleransiTerlambat;

    private Integer toleransiPulangAwal;

    private Boolean lintasHari;

    private Boolean aktif;
}