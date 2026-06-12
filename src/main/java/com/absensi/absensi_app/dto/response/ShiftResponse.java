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
    private Set<String> hariKerja;
}
