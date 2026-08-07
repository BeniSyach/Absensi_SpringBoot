package com.absensi.absensi_app.service.impl;

import com.absensi.absensi_app.dto.request.LoginRequest;
import com.absensi.absensi_app.dto.response.*;
import com.absensi.absensi_app.entity.User;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.repository.UserRepository;
import com.absensi.absensi_app.service.RedisTokenService;
import com.absensi.absensi_app.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    public LoginResponse login(LoginRequest request, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            log.warn("Login gagal untuk username: {} dari IP: {}", request.getUsername(), ipAddress);
            throw new AbsensiException("Username atau password salah");
        }

        User user = userRepository.findActiveByUsernameOrNip(request.getUsername())
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan atau tidak aktif"));

        // Update device ID jika ada
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            user.setDeviceId(request.getDeviceId());
            userRepository.save(user);
        }

        String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
        String accessToken = jwtUtil.generateToken(
                user.getUsername(), user.getRole().name(), user.getId(), deviceId);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Simpan token ke Redis (overwrite token lama = single session per device)
        long expiresIn = jwtUtil.getExpirationTime(accessToken);
        redisTokenService.simpanToken(user.getId(), deviceId, accessToken, expiresIn);

        log.info("User {} login berhasil dari IP {}", user.getUsername(), ipAddress);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn / 1000) // dalam detik
                .user(mapUserResponse(user))
                .build();
    }

    public void logout(String token, Long userId, String deviceId) {
        long ttl = jwtUtil.getExpirationTime(token);
        if (ttl > 0) {
            redisTokenService.blacklistToken(token, ttl);
        }
        redisTokenService.hapusToken(userId, deviceId);
        log.info("User {} logout", userId);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new AbsensiException("Refresh token sudah expired, silakan login ulang");
        }
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));

        String deviceId = user.getDeviceId() != null ? user.getDeviceId() : "default";
        String newAccessToken = jwtUtil.generateToken(
                user.getUsername(), user.getRole().name(), user.getId(), deviceId);
        long expiresIn = jwtUtil.getExpirationTime(newAccessToken);
        redisTokenService.simpanToken(user.getId(), deviceId, newAccessToken, expiresIn);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn / 1000)
                .user(mapUserResponse(user))
                .build();
    }

    private UserResponse mapUserResponse(User user) {
        OpdResponse opdResponse = null;
        if (user.getOpd() != null) {
            opdResponse = OpdResponse.builder()
                    .id(user.getOpd().getId())
                    .kode(user.getOpd().getKode())
                    .nama(user.getOpd().getNama())
                    .latitudeKantor(user.getOpd().getLatitudeKantor())
                    .longitudeKantor(user.getOpd().getLongitudeKantor())
                    .radiusAbsen(user.getOpd().getRadiusAbsen())
                    .build();
        }
        return UserResponse.builder()
                .id(user.getId())
                .nip(user.getNip())
                .username(user.getUsername())
                .namaLengkap(user.getNamaLengkap())
                .email(user.getEmail())
                .telepon(user.getTelepon())
                .fotoProfil(user.getFotoProfil())
                .role(user.getRole().name())
                .opd(opdResponse)
                .build();
    }
}
