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

    private final UserRepository userRepository;
    private final OpdRepository opdRepository;
    private final ShiftRepository shiftRepository;
    private final WaktuKerjaRepository waktuKerjaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            log.info("Data awal sudah ada, seeder dilewati.");
            return;
        }

        log.info("Membuat data awal...");

        // === 1. Buat OPD Contoh ===
        Opd opd = Opd.builder()
                .kode("SEKRETARIAT")
                .nama("Sekretariat Daerah")
                .alamat("Jl. Kapten Maulana Lubis No.2, Medan")
                // Koordinat Balai Kota Medan sebagai contoh
                .latitudeKantor(3.5952)
                .longitudeKantor(98.6722)
                .radiusAbsen(100)
                .aktif(true)
                .build();
        opd = opdRepository.save(opd);

        Opd opdDinas = Opd.builder()
                .kode("DISHUB")
                .nama("Dinas Perhubungan")
                .alamat("Jl. Pintu Air IV, Medan")
                .latitudeKantor(3.5870)
                .longitudeKantor(98.6800)
                .radiusAbsen(150)
                .aktif(true)
                .build();
        opdDinas = opdRepository.save(opdDinas);

        // === 2. Buat Shift ===
        Shift shiftPagi = Shift.builder()
                .nama("Shift Pagi")
                .jamMasuk(LocalTime.of(7, 30))
                .jamPulang(LocalTime.of(16, 0))
                .toleransiTerlambat(15)
                .toleransiPulangAwal(10)
                .opd(opd)
                .aktif(true)
                .build();
        shiftPagi = shiftRepository.save(shiftPagi);

        Shift shiftSiang = Shift.builder()
                .nama("Shift Siang")
                .jamMasuk(LocalTime.of(12, 0))
                .jamPulang(LocalTime.of(20, 0))
                .toleransiTerlambat(15)
                .toleransiPulangAwal(10)
                .opd(opd)
                .aktif(true)
                .build();
        shiftRepository.save(shiftSiang);

        // === 3. Buat User Admin ===
        User admin = User.builder()
                .nip("000000000000000001")
                .username("admin")
                .password(passwordEncoder.encode("Admin123!"))
                .namaLengkap("Administrator Sistem")
                .email("admin@absensi.go.id")
                .telepon("0811111111")
                .opd(opd)
                .role(Role.ROLE_ADMIN)
                .aktif(true)
                .build();
        admin = userRepository.save(admin);

        // === 4. Buat User Pimpinan ===
        User pimpinan = User.builder()
                .nip("198501012010011001")
                .username("pimpinan")
                .password(passwordEncoder.encode("Pimpinan123!"))
                .namaLengkap("Kepala Dinas")
                .email("pimpinan@absensi.go.id")
                .telepon("0822222222")
                .opd(opd)
                .role(Role.ROLE_PIMPINAN)
                .aktif(true)
                .build();
        pimpinan = userRepository.save(pimpinan);

        // === 5. Buat User Pegawai Contoh ===
        User pegawai = User.builder()
                .nip("199001012020121001")
                .username("budi.santoso")
                .password(passwordEncoder.encode("User123!"))
                .namaLengkap("Budi Santoso")
                .email("budi.santoso@absensi.go.id")
                .telepon("0833333333")
                .opd(opd)
                .role(Role.ROLE_USER)
                .aktif(true)
                .build();
        pegawai = userRepository.save(pegawai);

        // === 6. Assign Shift ke Pegawai ===
        WaktuKerja waktuKerja = WaktuKerja.builder()
                .user(pegawai)
                .shift(shiftPagi)
                .hariKerja(Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY))
                .tanggalMulai(LocalDate.now().withDayOfYear(1))
                .aktif(true)
                .build();
        waktuKerjaRepository.save(waktuKerja);

        log.info("========================================");
        log.info("✅ Data awal berhasil dibuat!");
        log.info("🔐 Akun yang tersedia:");
        log.info("   Admin    → username: admin        | password: Admin123!");
        log.info("   Pimpinan → username: pimpinan     | password: Pimpinan123!");
        log.info("   Pegawai  → username: budi.santoso | password: User123!");
        log.info("📖 Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("========================================");
    }
}
