package com.absensi.absensi_app.service.impl;

import com.absensi.absensi_app.dto.response.OpdResponse;
import com.absensi.absensi_app.dto.response.ShiftResponse;
import com.absensi.absensi_app.dto.response.WaktuKerjaResponse;
import com.absensi.absensi_app.repository.OpdRepository;
import com.absensi.absensi_app.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminService {

    private final OpdRepository opdRepository;
    private final ShiftRepository shiftRepository;


    public List<OpdResponse> getAllOpd() {

        return opdRepository.findByAktifTrue()
                .stream()
                .map(opd -> OpdResponse.builder()
                        .id(opd.getId())
                        .kode(opd.getKode())
                        .nama(opd.getNama())
                        .alamat(opd.getAlamat())
                        .latitudeKantor(opd.getLatitudeKantor())
                        .longitudeKantor(opd.getLongitudeKantor())
                        .radiusAbsen(opd.getRadiusAbsen())
                        .build())
                .toList();
    }


    public List<ShiftResponse> getShiftByOpd(Long opdId) {

        return shiftRepository.findByOpdIdAndAktifTrue(opdId)
                .stream()
                .map(shift -> ShiftResponse.builder()
                        .id(shift.getId())
                        .nama(shift.getNama())
                        .namaOpd(shift.getOpd().getNama())
                        .waktuKerja(
                                shift.getWaktuKerja()
                                        .stream()
                                        .filter(w -> Boolean.TRUE.equals(w.getAktif()))
                                        .map(w -> WaktuKerjaResponse.builder()
                                                .id(w.getId())
                                                .hari(w.getHari())
                                                .jamMasuk(w.getJamMasuk())
                                                .jamPulang(w.getJamPulang())
                                                .toleransiTerlambat(w.getToleransiTerlambat())
                                                .toleransiPulangAwal(w.getToleransiPulangAwal())
                                                .lintasHari(w.getLintasHari())
                                                .build())
                                        .toList()
                        )
                        .build())
                .toList();
    }
}