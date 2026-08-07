package com.absensi.absensi_app.service.impl;

import com.absensi.absensi_app.dto.request.*;
import com.absensi.absensi_app.dto.response.*;
import com.absensi.absensi_app.entity.*;
import com.absensi.absensi_app.enums.Role;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.repository.*;
import com.absensi.absensi_app.service.FotoService;
import com.absensi.absensi_app.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OpdRepository opdRepository;
    private final WaktuKerjaRepository waktuKerjaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FotoService fotoService;
    private final RedisTokenService redisTokenService;
    private final ShiftRepository shiftRepository;

    // =============================================
    // REGISTRASI MANDIRI (user baru daftar sendiri)
    // =============================================

    @Transactional
    public UserDetailResponse register(RegisterRequest request) {
        // Validasi kecocokan password
        if (!request.getPassword().equals(request.getKonfirmasiPassword())) {
            throw new AbsensiException("Password dan konfirmasi password tidak cocok");
        }

        // Cek duplikat
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AbsensiException("Username '" + request.getUsername() + "' sudah digunakan");
        }
        if (userRepository.existsByNip(request.getNip())) {
            throw new AbsensiException("NIP '" + request.getNip() + "' sudah terdaftar");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new AbsensiException("Email sudah digunakan");
        }

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));

        if (!opd.getAktif()) {
            throw new AbsensiException("OPD yang dipilih tidak aktif");
        }

        User user = User.builder()
                .nip(request.getNip())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .namaLengkap(request.getNamaLengkap())
                .email(request.getEmail())
                .telepon(request.getTelepon())
                .opd(opd)
                .role(Role.ROLE_USER)   // registrasi mandiri selalu USER
                .aktif(false)           // menunggu aktivasi admin
                .deviceId(request.getDeviceId())
                .build();

        User saved = userRepository.save(user);
        log.info("Registrasi baru: {} (NIP: {}) - menunggu aktivasi admin", saved.getUsername(), saved.getNip());

        return mapToDetail(saved);
    }

    // =============================================
    // PROFIL USER SENDIRI
    // =============================================

    public UserDetailResponse getProfilSaya(Long userId) {
        User user = findUserById(userId);
        return mapToDetail(user);
    }

    @Transactional
    public UserDetailResponse updateProfil(Long userId, UpdateProfilRequest request) {
        User user = findUserById(userId);

        // Cek email duplikat (kecuali email sendiri)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            Optional<User> existing = userRepository.findByUsername(user.getUsername());
            if (existing.isPresent() && !existing.get().getId().equals(userId)
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new AbsensiException("Email sudah digunakan user lain");
            }
        }

        user.setNamaLengkap(request.getNamaLengkap());
        user.setEmail(request.getEmail());
        user.setTelepon(request.getTelepon());

        User saved = userRepository.save(user);
        log.info("User {} update profil", userId);
        return mapToDetail(saved);
    }

    @Transactional
    public void gantiPassword(Long userId, GantiPasswordRequest request) {
        if (!request.getPasswordBaru().equals(request.getKonfirmasiPasswordBaru())) {
            throw new AbsensiException("Password baru dan konfirmasi tidak cocok");
        }

        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getPasswordLama(), user.getPassword())) {
            throw new AbsensiException("Password lama tidak sesuai");
        }

        if (passwordEncoder.matches(request.getPasswordBaru(), user.getPassword())) {
            throw new AbsensiException("Password baru tidak boleh sama dengan password lama");
        }

        user.setPassword(passwordEncoder.encode(request.getPasswordBaru()));
        userRepository.save(user);

        // Paksa logout semua sesi setelah ganti password
        String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
        redisTokenService.hapusToken(userId, deviceId);

        log.info("User {} berhasil ganti password, semua sesi dihapus", userId);
    }

    @Transactional
    public UserDetailResponse uploadFotoProfil(Long userId, MultipartFile foto) {
        User user = findUserById(userId);

        // Hapus foto lama jika ada
        if (user.getFotoProfil() != null) {
            fotoService.hapusFoto(user.getFotoProfil());
        }

        try {
            String pathFoto = fotoService.uploadFoto(foto, userId, "profil");
            user.setFotoProfil(pathFoto);
            User saved = userRepository.save(user);
            log.info("User {} update foto profil", userId);
            return mapToDetail(saved);
        } catch (IOException e) {
            throw new AbsensiException("Gagal mengupload foto profil: " + e.getMessage());
        }
    }

    @Transactional
    public void hapusFotoProfil(Long userId) {
        User user = findUserById(userId);
        if (user.getFotoProfil() != null) {
            fotoService.hapusFoto(user.getFotoProfil());
            user.setFotoProfil(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public void updateDeviceId(Long userId, String deviceId) {
        User user = findUserById(userId);
        String deviceIdLama = user.getDeviceId();

        // Hapus token sesi lama jika device berubah
        if (deviceIdLama != null && !deviceIdLama.equals(deviceId)) {
            redisTokenService.hapusToken(userId, deviceIdLama);
            log.info("Device ID user {} berubah, sesi lama dihapus", userId);
        }

        user.setDeviceId(deviceId);
        userRepository.save(user);
    }

    // =============================================
    // MANAJEMEN USER OLEH ADMIN
    // =============================================
    public PageResponse<UserDetailResponse> daftarUser(
            String keyword,
            Long opdId,
            Boolean aktif,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("namaLengkap").ascending());

        Specification<User> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {

            String search = "%" + keyword.toLowerCase() + "%";

            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("namaLengkap")), search),
                    cb.like(cb.lower(root.get("username")), search),
                    cb.like(root.get("nip"), "%" + keyword + "%")
            ));
        }

        if (opdId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("opd").get("id"), opdId));
        }

        if (aktif != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("aktif"), aktif));
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);

        Page<UserDetailResponse> responsePage =
                userPage.map(this::mapToDetail);

        return PageResponse.of(responsePage);
    }

    public UserDetailResponse getDetailUser(Long userId) {

        User user = userRepository.findDetailById(userId)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));

        return mapToDetail(user);
    }

    @Transactional
    public UserDetailResponse adminTambahUser(RegisterRequest request, Role role) {
        if (!request.getPassword().equals(request.getKonfirmasiPassword())) {
            throw new AbsensiException("Password dan konfirmasi tidak cocok");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AbsensiException("Username sudah digunakan");
        }

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));

        Shift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() ->
                        new RuntimeException("Shift tidak ditemukan")
                );

        User user = User.builder()
                .nip(request.getNip())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .namaLengkap(request.getNamaLengkap())
                .email(request.getEmail())
                .telepon(request.getTelepon())
                .opd(opd)
                .shift(shift)
                .role(role != null ? role : Role.ROLE_USER)
                .aktif(true)  // admin buat user langsung aktif
                .deviceId(request.getDeviceId())
                .build();

        User saved = userRepository.save(user);
        log.info("Admin membuat user baru: {} ({})", saved.getUsername(), saved.getRole());
        return mapToDetail(saved);
    }

    @Transactional
    public UserDetailResponse adminUpdateUser(Long userId, AdminUpdateUserRequest request) {
        User user = findUserById(userId);

        // Cek NIP duplikat kecuali milik sendiri
        if (!user.getNip().equals(request.getNip()) && userRepository.existsByNip(request.getNip())) {
            throw new AbsensiException("NIP sudah digunakan user lain");
        }

        if(request.getShiftId() != null){

            Shift shift = shiftRepository.findById(
                    request.getShiftId()
            ).orElseThrow(
                    () -> new RuntimeException("Shift tidak ditemukan")
            );

            user.setShift(shift);
        }

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));

        user.setNip(request.getNip());
        user.setNamaLengkap(request.getNamaLengkap());
        user.setEmail(request.getEmail());
        user.setTelepon(request.getTelepon());
        user.setOpd(opd);

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getAktif() != null) {
            user.setAktif(request.getAktif());
            // Jika dinonaktifkan, paksa logout
            if (!request.getAktif()) {
                String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
                redisTokenService.hapusToken(userId, deviceId);
            }
        }

        User saved = userRepository.save(user);
        log.info("Admin update user: {}", userId);
        return mapToDetail(saved);
    }

    @Transactional
    public void adminResetPassword(Long userId, ResetPasswordRequest request) {
        if (!request.getPasswordBaru().equals(request.getKonfirmasiPasswordBaru())) {
            throw new AbsensiException("Password baru dan konfirmasi tidak cocok");
        }

        User user = findUserById(userId);
        user.setPassword(passwordEncoder.encode(request.getPasswordBaru()));
        userRepository.save(user);

        // Paksa logout user setelah password direset
        String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
        redisTokenService.hapusToken(userId, deviceId);

        log.info("Admin reset password user: {}", userId);
    }

    @Transactional
    public void adminAktivasiUser(Long userId, boolean aktif) {
        User user = findUserById(userId);
        user.setAktif(aktif);
        userRepository.save(user);

        if (!aktif) {
            String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
            redisTokenService.hapusToken(userId, deviceId);
        }

        log.info("Admin {} user: {}", aktif ? "aktifkan" : "nonaktifkan", userId);
    }

    @Transactional
    public void adminHapusDeviceId(Long userId) {
        User user = findUserById(userId);
        String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
        // Hapus sesi aktif
        redisTokenService.hapusToken(userId, deviceId);
        user.setDeviceId(null);
        userRepository.save(user);
        log.info("Admin hapus device ID user: {}", userId);
    }

    @Transactional
    public void adminPaksiLogout(Long userId) {
        User user = findUserById(userId);
        String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
        redisTokenService.hapusToken(userId, deviceId);
        log.info("Admin paksa logout user: {}", userId);
    }

    // =============================================
    // HELPERS
    // =============================================

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));
    }

    public UserDetailResponse mapToDetail(User user) {

        // ✅ 1. SHIFT MASTER yang di-assign ke user (untuk dropdown edit)
        ShiftResponse shiftResponse = null;
        if (user.getShift() != null) {
            Shift shift = user.getShift();
            List<WaktuKerjaResponse> listWaktuKerja = null;
            if (shift.getWaktuKerja() != null) {
                listWaktuKerja = shift.getWaktuKerja().stream()
                        .map(wk -> WaktuKerjaResponse.builder()
                                .id(wk.getId())
                                .hari(wk.getHari())
                                .jamMasuk(wk.getJamMasuk())
                                .jamPulang(wk.getJamPulang())
                                .toleransiTerlambat(wk.getToleransiTerlambat())
                                .toleransiPulangAwal(wk.getToleransiPulangAwal())
                                .lintasHari(wk.getLintasHari())
                                .aktif(wk.getAktif())
                                .build())
                        .collect(Collectors.toList());
            }

            shiftResponse = ShiftResponse.builder()
                    .id(shift.getId())
                    .nama(shift.getNama())
                    .aktif(shift.getAktif())
                    .opdId(shift.getOpd() != null ? shift.getOpd().getId() : null)
                    .namaOpd(shift.getOpd() != null ? shift.getOpd().getNama() : null)
                    .waktuKerja(listWaktuKerja)
                    .build();
        }

        // ✅ 3. OPD
        OpdResponse opdResponse = null;
        if (user.getOpd() != null) {
            opdResponse = OpdResponse.builder()
                    .id(user.getOpd().getId())
                    .kode(user.getOpd().getKode())
                    .nama(user.getOpd().getNama())
                    .alamat(user.getOpd().getAlamat())
                    .latitudeKantor(user.getOpd().getLatitudeKantor())
                    .longitudeKantor(user.getOpd().getLongitudeKantor())
                    .radiusAbsen(user.getOpd().getRadiusAbsen())
                    .build();
        }

        return UserDetailResponse.builder()
                .id(user.getId())
                .nip(user.getNip())
                .username(user.getUsername())
                .namaLengkap(user.getNamaLengkap())
                .email(user.getEmail())
                .telepon(user.getTelepon())
                .fotoProfil(user.getFotoProfil())
                .role(user.getRole().name())
                .aktif(user.getAktif())
                .deviceId(user.getDeviceId())
                .opd(opdResponse)
                .shift(shiftResponse)            // ← Shift master user
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public List<ShiftResponse> daftarShift(Long opdId) {

        List<Shift> shifts;

        if (opdId != null) {
            shifts = shiftRepository.findByOpdIdAndAktifTrue(opdId);
        } else {
            shifts = shiftRepository.findByAktifTrue();
        }


        return shifts.stream()
                .map(shift -> ShiftResponse.builder()
                        .id(shift.getId())
                        .nama(shift.getNama())
                        .aktif(shift.getAktif())
                        .build()
                )
                .toList();
    }
}
