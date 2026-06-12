package com.absensi.absensi_app.filter;

import com.absensi.absensi_app.service.CustomUserDetailsService;
import com.absensi.absensi_app.service.RedisTokenService;
import com.absensi.absensi_app.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 1. Cek blacklist (sudah logout)
            if (redisTokenService.isTokenBlacklisted(token)) {
                log.debug("Token di-blacklist, akses ditolak");
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);
            String deviceId = jwtUtil.extractDeviceId(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 2. Validasi token masih valid
                if (jwtUtil.isTokenValid(token, username)) {

                    // 3. Verifikasi token di Redis (cegah token lama setelah re-login)
                    String storedToken = redisTokenService.ambilToken(userId, deviceId);
                    if (storedToken == null || !storedToken.equals(token)) {
                        log.warn("Token tidak match di Redis untuk user {}", username);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Tambahkan info ke request attribute untuk controller
                    request.setAttribute("userId", userId);
                    request.setAttribute("deviceId", deviceId);
                }
            }
        } catch (Exception e) {
            log.debug("JWT validation error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
