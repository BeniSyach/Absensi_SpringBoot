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
import java.time.LocalTime;
import java.util.List;

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


        // ==========================
        // 1. OPD
        // ==========================

        Opd opd = opdRepository.save(
                Opd.builder()
                        .kode("SEKRETARIAT")
                        .nama("Sekretariat Daerah")
                        .alamat("Jl. Kapten Maulana Lubis No.2, Medan")
                        .latitudeKantor(3.5952)
                        .longitudeKantor(98.6722)
                        .radiusAbsen(100)
                        .aktif(true)
                        .build()
        );


        // ==========================
        // 2. SHIFT PAGI
        // ==========================

        Shift shiftPagi = shiftRepository.save(
                Shift.builder()
                        .nama("Shift Pagi")
                        .opd(opd)
                        .aktif(true)
                        .build()
        );


        buatWaktuKerja(
                shiftPagi,
                List.of(
                        "Senin",
                        "Selasa",
                        "Rabu",
                        "Kamis",
                        "Jumat"
                ),
                LocalTime.of(7,30),
                LocalTime.of(16,0)
        );


        // ==========================
        // 3. SHIFT SIANG
        // ==========================

        Shift shiftSiang = shiftRepository.save(
                Shift.builder()
                        .nama("Shift Siang")
                        .opd(opd)
                        .aktif(true)
                        .build()
        );


        buatWaktuKerja(
                shiftSiang,
                List.of(
                        "Senin",
                        "Selasa",
                        "Rabu",
                        "Kamis",
                        "Jumat"
                ),
                LocalTime.of(12,0),
                LocalTime.of(20,0)
        );


        // ==========================
        // 4. SHIFT MALAM
        // ==========================

        Shift shiftMalam = shiftRepository.save(
                Shift.builder()
                        .nama("Shift Malam")
                        .opd(opd)
                        .aktif(true)
                        .build()
        );


        buatWaktuKerja(
                shiftMalam,
                List.of(
                        "Senin",
                        "Selasa",
                        "Rabu",
                        "Kamis",
                        "Jumat"
                ),
                LocalTime.of(20,0),
                LocalTime.of(4,0)
        );


        // ==========================
        // 5. USER ADMIN
        // ==========================

        userRepository.save(
                User.builder()
                        .nip("000000000000000001")
                        .username("admin")
                        .password(passwordEncoder.encode("Admin123!"))
                        .namaLengkap("Administrator Sistem")
                        .email("admin@absensi.go.id")
                        .opd(opd)
                        .shift(shiftPagi)
                        .role(Role.ROLE_ADMIN)
                        .aktif(true)
                        .build()
        );


        // ==========================
        // 6. USER PIMPINAN
        // ==========================

        userRepository.save(
                User.builder()
                        .nip("198501012010011001")
                        .username("pimpinan")
                        .password(passwordEncoder.encode("Pimpinan123!"))
                        .namaLengkap("Kepala Dinas")
                        .email("pimpinan@absensi.go.id")
                        .opd(opd)
                        .shift(shiftPagi)
                        .role(Role.ROLE_PIMPINAN)
                        .aktif(true)
                        .build()
        );


        // ==========================
        // 7. USER PEGAWAI
        // ==========================

        String[] nips = {
                "199001012020121001",
                "199501012021121001",
                "200001012022121001"
        };

        String[] names = {
                "Budi Santoso",
                "Siti Rahayu",
                "Ahmad Fauzi"
        };

        String[] usernames = {
                "budi.santoso",
                "siti.rahayu",
                "ahmad.fauzi"
        };


        for (int i = 0; i < nips.length; i++) {

            userRepository.save(
                    User.builder()
                            .nip(nips[i])
                            .username(usernames[i])
                            .password(passwordEncoder.encode("User123!"))
                            .namaLengkap(names[i])
                            .opd(opd)
                            .shift(shiftPagi)
                            .role(Role.ROLE_USER)
                            .aktif(true)
                            .build()
            );
        }


        log.info("========================================");
        log.info("✅ Data awal berhasil dibuat");
        log.info("Admin    : admin / Admin123!");
        log.info("Pimpinan : pimpinan / Pimpinan123!");
        log.info("User     : budi.santoso / User123!");
        log.info("========================================");
    }


    private void buatWaktuKerja(
            Shift shift,
            List<String > hariKerja,
            LocalTime masuk,
            LocalTime pulang
    ) {

        boolean lintasHari = pulang.isBefore(masuk);


        hariKerja.forEach(hari -> {

            waktuKerjaRepository.save(
                    WaktuKerja.builder()
                            .shift(shift)
                            .hari(hari)
                            .jamMasuk(masuk)
                            .jamPulang(pulang)
                            .toleransiTerlambat(15)
                            .toleransiPulangAwal(10)
                            .lintasHari(lintasHari)
                            .aktif(true)
                            .build()
            );

        });
    }
}