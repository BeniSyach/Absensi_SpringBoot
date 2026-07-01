package com.absensi.absensi_app.config;

import com.absensi.absensi_app.entity.*;
import com.absensi.absensi_app.enums.Role;
import com.absensi.absensi_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository     userRepository;
    private final OpdRepository      opdRepository;
    private final ShiftRepository    shiftRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            log.info("Data awal sudah ada, seeder dilewati.");
            return;
        }

        log.info("Membuat data awal...");

        // ── 1. OPD ──
        Opd opd = opdRepository.save(Opd.builder()
                .kode("SEKRETARIAT")
                .nama("Sekretariat Daerah")
                .alamat("Jl. Kapten Maulana Lubis No.2, Medan")
                .latitudeKantor(3.5952)
                .longitudeKantor(98.6722)
                .radiusAbsen(100)
                .aktif(true)
                .build());

        // ── 2. Tiga shift — lintasHari dihitung otomatis ──

        // Shift Pagi: 07:30 → 16:00, lintasHari = false
        shiftRepository.save(Shift.builder()
                .nama("Shift Pagi")
                .jamMasuk(LocalTime.of(7, 30))
                .jamPulang(LocalTime.of(16, 0))
                .toleransiTerlambat(15)
                .toleransiPulangAwal(10)
                .opd(opd)
                .aktif(true)
                .build());

        // Shift Siang: 12:00 → 20:00, lintasHari = false
        shiftRepository.save(Shift.builder()
                .nama("Shift Siang")
                .jamMasuk(LocalTime.of(12, 0))
                .jamPulang(LocalTime.of(20, 0))
                .toleransiTerlambat(15)
                .toleransiPulangAwal(10)
                .opd(opd)
                .aktif(true)
                .build());

        // Shift Malam: 20:00 → 04:00, lintasHari = TRUE (04:00 < 20:00)
        shiftRepository.save(Shift.builder()
                .nama("Shift Malam")
                .jamMasuk(LocalTime.of(20, 0))
                .jamPulang(LocalTime.of(4, 0))
                .toleransiTerlambat(15)
                .toleransiPulangAwal(10)
                .opd(opd)
                .aktif(true)
                .build());

        // ── 3. User admin ──
        userRepository.save(User.builder()
                .nip("000000000000000001")
                .username("admin")
                .password(passwordEncoder.encode("Admin123!"))
                .namaLengkap("Administrator Sistem")
                .email("admin@absensi.go.id")
                .opd(opd)
                .role(Role.ROLE_ADMIN)
                .aktif(true)
                .build());

        // ── 4. User pimpinan ──
        userRepository.save(User.builder()
                .nip("198501012010011001")
                .username("pimpinan")
                .password(passwordEncoder.encode("Pimpinan123!"))
                .namaLengkap("Kepala Dinas")
                .email("pimpinan@absensi.go.id")
                .opd(opd)
                .role(Role.ROLE_PIMPINAN)
                .aktif(true)
                .build());

        // ── 5. Beberapa pegawai contoh ──
        String[] nips  = {"199001012020121001", "199501012021121001", "200001012022121001"};
        String[] names = {"Budi Santoso", "Siti Rahayu", "Ahmad Fauzi"};
        String[] users = {"budi.santoso", "siti.rahayu", "ahmad.fauzi"};

        for (int i = 0; i < nips.length; i++) {
            userRepository.save(User.builder()
                    .nip(nips[i])
                    .username(users[i])
                    .password(passwordEncoder.encode("User123!"))
                    .namaLengkap(names[i])
                    .opd(opd)
                    .role(Role.ROLE_USER)
                    .aktif(true)
                    .build());
        }

        log.info("========================================");
        log.info("✅ Data awal berhasil dibuat!");
        log.info("");
        log.info("🔐 Akun:");
        log.info("  Admin    → admin        / Admin123!");
        log.info("  Pimpinan → pimpinan     / Pimpinan123!");
        log.info("  Pegawai  → budi.santoso / User123!");
        log.info("");
        log.info("🕐 Shift yang dibuat (pegawai pilih sendiri saat absen):");
        log.info("  🌅 Shift Pagi  : 07:30–16:00 (tidak lintas hari)");
        log.info("  ☀ Shift Siang : 12:00–20:00 (tidak lintas hari)");
        log.info("  🌙 Shift Malam : 20:00–04:00 (LINTAS HARI)");
        log.info("");
        log.info("📖 Swagger: http://localhost:8080/swagger-ui.html");
        log.info("========================================");
    }
}