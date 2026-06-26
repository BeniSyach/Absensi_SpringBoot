package com.absensi.absensi_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShiftResponse {
    private Long id;
    private String nama;
    private LocalTime jamMasuk;
    private LocalTime jamPulang;
    private Integer toleransiTerlambat;
    private Integer toleransiPulangAwal;
    private Boolean lintasHari;
    private Set<String> hariKerja;
    private Boolean aktif;
    private Long opdId;
    private String namaOpd;

    public String getLabel() {
        String suffix = Boolean.TRUE.equals(lintasHari) ? " (lintas hari)" : "";
        return nama + " · " + jamMasuk + " – " + jamPulang + suffix;
    }
}
