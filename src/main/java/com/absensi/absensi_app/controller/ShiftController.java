package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.dto.response.ShiftDetailResponse;
import com.absensi.absensi_app.dto.response.WaktuKerjaResponse;
import com.absensi.absensi_app.entity.Shift;
import com.absensi.absensi_app.repository.ShiftRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shift")
@RequiredArgsConstructor
@Tag(name = "Shift")
public class ShiftController {

    private final ShiftRepository shiftRepository;

    @GetMapping
    @Operation(summary = "Daftar shift beserta waktu kerja")
    public ResponseEntity<ApiResponse<List<ShiftDetailResponse>>> getAll() {

        List<ShiftDetailResponse> result = shiftRepository.findAll()
                .stream()
                .map(this::mapShift)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.sukses(result, "Data shift")
        );
    }

    private ShiftDetailResponse mapShift(Shift shift){

        return ShiftDetailResponse.builder()
                .id(shift.getId())
                .nama(shift.getNama())
                .aktif(shift.getAktif())
                .waktuKerja(
                        shift.getWaktuKerja()
                                .stream()
                                .map(w -> WaktuKerjaResponse.builder()
                                        .id(w.getId())
                                        .hari(w.getHari())
                                        .jamMasuk(w.getJamMasuk())
                                        .jamPulang(w.getJamPulang())
                                        .toleransiTerlambat(w.getToleransiTerlambat())
                                        .toleransiPulangAwal(w.getToleransiPulangAwal())
                                        .lintasHari(w.getLintasHari())
                                        .aktif(w.getAktif())
                                        .build())
                                .toList()
                )
                .build();
    }

}