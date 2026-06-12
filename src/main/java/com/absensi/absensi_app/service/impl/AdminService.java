package com.absensi.absensi_app.service.impl;

import com.absensi.absensi_app.dto.response.OpdResponse;
import com.absensi.absensi_app.dto.response.ShiftResponse;
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
                .map(s -> ShiftResponse.builder()
                        .id(s.getId())
                        .nama(s.getNama())
                        .jamMasuk(s.getJamMasuk())
                        .jamPulang(s.getJamPulang())
                        .toleransiTerlambat(s.getToleransiTerlambat())
                        .toleransiPulangAwal(s.getToleransiPulangAwal())
                        .build()
                )
                .toList();
    }
}
