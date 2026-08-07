package com.absensi.absensi_app.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class WaktuKerjaRequest {

    @NotNull(message = "Hari wajib dipilih")
    private String hari;


    @NotNull(message = "Jam masuk wajib diisi")
    private LocalTime jamMasuk;


    @NotNull(message = "Jam pulang wajib diisi")
    private LocalTime jamPulang;


    @Min(value = 0, message = "Toleransi tidak boleh negatif")
    @Max(value = 120, message = "Toleransi maksimal 120 menit")
    private Integer toleransiTerlambat = 15;


    @Min(value = 0, message = "Toleransi tidak boleh negatif")
    @Max(value = 120, message = "Toleransi maksimal 120 menit")
    private Integer toleransiPulangAwal = 10;


    private Boolean aktif = true;
}