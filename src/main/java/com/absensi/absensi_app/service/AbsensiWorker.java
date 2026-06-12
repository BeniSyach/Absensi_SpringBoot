package com.absensi.absensi_app.service;

import com.absensi.absensi_app.entity.*;
import com.absensi.absensi_app.enums.StatusAbsensi;
import com.absensi.absensi_app.repository.*;
import com.absensi.absensi_app.service.AbsensiQueueService.AbsenPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Worker yang mengambil item dari Redis queue dan batch-insert ke PostgreSQL.
 *
 * KEUNGGULAN batch insert dibanding insert satu-per-satu:
 * - 50 insert terpisah: ~50 round-trip ke DB = ~500ms
 * - 1 batch insert 50 row: ~1 round-trip = ~20ms
 *
 * Worker jalan tiap 500ms — saat peak traffic, akan terus menguras queue.
 * Saat sepi, tidak ada pekerjaan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbsensiWorker {

    private final AbsensiQueueService queueService;
    private final AbsenMasukRepository absenMasukRepository;
    private final AbsenPulangRepository absenPulangRepository;
    private final UserRepository userRepository;
    private final OpdRepository opdRepository;
    private final ShiftRepository shiftRepository;
    private final ObjectMapper objectMapper;

    /**
     * Proses queue absen masuk setiap 500ms.
     * Ambil batch 50 item, batch insert ke PostgreSQL.
     */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void prosesQueueMasuk() {
        List<String> batch = queueService.dequeueBatch(
                queueService.getMasukQueue(), queueService.getBatchSize());
        if (batch.isEmpty()) return;

        List<AbsenMasuk> toSave = new ArrayList<>();
        for (String json : batch) {
            try {
                AbsenPayload payload = objectMapper.readValue(json, AbsenPayload.class);
                AbsenMasuk absen = buildAbsenMasuk(payload);
                if (absen != null) toSave.add(absen);
            } catch (Exception e) {
                log.error("Gagal proses item queue masuk: {}", e.getMessage());
                queueService.pindahKeDlq(json, queueService.getDlqMasuk());
            }
        }

        if (!toSave.isEmpty()) {
            absenMasukRepository.saveAll(toSave);
            log.debug("Batch insert {} absen masuk ke DB", toSave.size());
            toSave.forEach(a -> queueService.incrementProcessed());
        }
    }

    /**
     * Proses queue absen pulang setiap 500ms.
     */
    @Scheduled(fixedDelay = 500, initialDelay = 250)
    @Transactional
    public void prosesQueuePulang() {
        List<String> batch = queueService.dequeueBatch(
                queueService.getPulangQueue(), queueService.getBatchSize());
        if (batch.isEmpty()) return;

        List<AbsenPulang> toSave = new ArrayList<>();
        for (String json : batch) {
            try {
                AbsenPayload payload = objectMapper.readValue(json, AbsenPayload.class);
                AbsenPulang absen = buildAbsenPulang(payload);
                if (absen != null) toSave.add(absen);
            } catch (Exception e) {
                log.error("Gagal proses item queue pulang: {}", e.getMessage());
                queueService.pindahKeDlq(json, queueService.getDlqPulang());
            }
        }

        if (!toSave.isEmpty()) {
            absenPulangRepository.saveAll(toSave);
            log.debug("Batch insert {} absen pulang ke DB", toSave.size());
        }
    }

    private AbsenMasuk buildAbsenMasuk(AbsenPayload p) {
        User user = userRepository.findById(p.userId).orElse(null);
        if (user == null) {
            log.warn("User {} tidak ditemukan saat proses queue", p.userId);
            return null;
        }

        LocalDate tanggal = Instant.ofEpochMilli(p.waktuMs)
                .atZone(ZoneId.of("Asia/Jakarta")).toLocalDate();
        LocalDateTime waktu = Instant.ofEpochMilli(p.waktuMs)
                .atZone(ZoneId.of("Asia/Jakarta")).toLocalDateTime();

        // Skip jika sudah ada (idempoten — penting untuk retry)
        if (absenMasukRepository.existsByUserIdAndTanggal(p.userId, tanggal)) {
            log.debug("Absen masuk sudah ada untuk user {} tanggal {}, skip", p.userId, tanggal);
            return null;
        }

        return AbsenMasuk.builder()
                .user(user)
                .opd(user.getOpd())
                .tanggal(tanggal)
                .waktuMasuk(waktu)
                .latitude(p.latitude)
                .longitude(p.longitude)
                .akurasiGps(p.akurasiGps)
                .jarakDariKantor(p.jarakDariKantor)
                .fotoAbsen(p.fotoPath)
                .lokasiValid(p.lokasiValid)
                .mockLocationDetected(p.mockDetected)
                .locationProvider(p.locationProvider)
                .kecepatanPerpindahan(p.kecepatanPerpindahan)
                .status(parseStatus(p.status))
                .ipAddress(p.ipAddress)
                .deviceInfo(p.deviceInfo)
                .catatan(p.catatan)
                .build();
    }

    private AbsenPulang buildAbsenPulang(AbsenPayload p) {
        User user = userRepository.findById(p.userId).orElse(null);
        if (user == null) return null;

        LocalDate tanggal = Instant.ofEpochMilli(p.waktuMs)
                .atZone(ZoneId.of("Asia/Jakarta")).toLocalDate();
        LocalDateTime waktu = Instant.ofEpochMilli(p.waktuMs)
                .atZone(ZoneId.of("Asia/Jakarta")).toLocalDateTime();

        if (absenPulangRepository.existsByUserIdAndTanggal(p.userId, tanggal)) {
            return null;
        }

        AbsenMasuk absenMasuk = absenMasukRepository
                .findByUserIdAndTanggal(p.userId, tanggal).orElse(null);

        int durasiMenit = 0;
        if (absenMasuk != null) {
            durasiMenit = (int) java.time.temporal.ChronoUnit.MINUTES
                    .between(absenMasuk.getWaktuMasuk(), waktu);
        }

        return AbsenPulang.builder()
                .user(user)
                .opd(user.getOpd())
                .absenMasuk(absenMasuk)
                .tanggal(tanggal)
                .waktuPulang(waktu)
                .latitude(p.latitude)
                .longitude(p.longitude)
                .akurasiGps(p.akurasiGps)
                .jarakDariKantor(p.jarakDariKantor)
                .fotoAbsen(p.fotoPath)
                .lokasiValid(p.lokasiValid)
                .mockLocationDetected(p.mockDetected)
                .locationProvider(p.locationProvider)
                .kecepatanPerpindahan(p.kecepatanPerpindahan)
                .status(parseStatus(p.status))
                .durasiKerjaMenit(durasiMenit)
                .ipAddress(p.ipAddress)
                .deviceInfo(p.deviceInfo)
                .catatan(p.catatan)
                .build();
    }

    private StatusAbsensi parseStatus(String s) {
        try { return StatusAbsensi.valueOf(s); }
        catch (Exception e) { return StatusAbsensi.HADIR; }
    }
}
